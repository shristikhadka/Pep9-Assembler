import java.io.*;
import java.util.*;

public class pepasm {
    // Store label positions
    private static HashMap<String, Integer> labels = new HashMap<>();

    // Track current position
    private static int currentPosition = 0;

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("error");
            System.exit(1);
        }

        try {
            // First  find all labels
            findLabels(args[0]);

            // Second - generate code
            ArrayList<String> machineCode = generateCode(args[0]);

            // Print the machine code
            for (int i = 0; i < machineCode.size(); i++) {
                System.out.print(machineCode.get(i));
                if (i < machineCode.size() - 1) {
                    System.out.print(" ");
                }
            }
            System.out.println();

        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void findLabels(String filename) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(filename));
        String line;
        currentPosition = 0;

        while ((line = reader.readLine()) != null) {
            // Remove comments
            if (line.contains(";")) {
                line = line.substring(0, line.indexOf(";"));
            }

            line = line.trim();
            if (line.isEmpty() || line.equals(".END")) {
                continue;
            }

            // Check if line has a label
            if (line.contains(":")) {
                String[] parts = line.split(":", 2);
                String labelName = parts[0].trim();
                labels.put(labelName, currentPosition);

                // If there's an instruction after the label, process it
                if (parts.length > 1 && !parts[1].trim().isEmpty()) {
                    line = parts[1].trim();
                } else {
                    continue;
                }
            }

            // Get instruction name
            String[] parts = line.split("\\s+", 2);
            String instruction = parts[0].toUpperCase();

            // Update position based on instruction type
            if (instruction.equals("STOP") || instruction.equals("ASLA") || instruction.equals("ASRA")) {
                currentPosition += 1;  // Single byte instruction
            } else {
                currentPosition += 3;  // Opcode + 2 byte operand
            }
        }

        reader.close();
    }

    private static ArrayList<String> generateCode(String filename) throws IOException {
        ArrayList<String> machineCode = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new FileReader(filename));
        String line;
        currentPosition = 0;

        while ((line = reader.readLine()) != null) {
            // Remove comments
            if (line.contains(";")) {
                line = line.substring(0, line.indexOf(";"));
            }

            line = line.trim();
            if (line.isEmpty() || line.equals(".END")) {
                continue;
            }

            // Handle labels
            if (line.contains(":")) {
                String[] parts = line.split(":", 2);
                if (parts.length > 1 && !parts[1].trim().isEmpty()) {
                    line = parts[1].trim();
                } else {
                    continue;
                }
            }

            // Split into instruction and operand
            String[] parts = line.split("\\s+", 2);
            String instruction = parts[0].toUpperCase();

            // Handle instructions with no operands
            if (instruction.equals("STOP")) {
                machineCode.add("00");
                currentPosition += 1;
                continue;
            } else if (instruction.equals("ASLA")) {
                machineCode.add("0A");
                currentPosition += 1;
                continue;
            } else if (instruction.equals("ASRA")) {
                machineCode.add("0C");
                currentPosition += 1;
                continue;
            }

            // Handle instructions with operands
            String operandPart = parts.length > 1 ? parts[1].trim() : "";
            String[] operandParts = operandPart.split(",");
            String operand = operandParts[0].trim();
            String addressingMode = operandParts.length > 1 ? operandParts[1].trim() : "i";

            // Handle branching to labels
            if (instruction.equals("BRNE") && labels.containsKey(operand)) {
                int targetAddress = labels.get(operand);
                int offset = targetAddress - (currentPosition + 3); // 3 bytes for the BRNE instruction
                // Convert to 16-bit two's complement
                offset = offset & 0xFFFF; // Keep only 16 bits
                operand = "0x" + Integer.toHexString(offset);
            }

            // Remove 0x prefix if present
            if (operand.startsWith("0x")) {
                operand = operand.substring(2);
            }

            // Convert to 4-digit hex
            int operandValue = Integer.parseInt(operand, 16);
            String operandHex = String.format("%04X", operandValue);

            // Add opcode based on instruction and addressing mode
            if (instruction.equals("LDBA")) {
                if (addressingMode.equals("i")) {
                    machineCode.add("D0");
                } else if (addressingMode.equals("d")) {
                    machineCode.add("D1");
                }
            } else if (instruction.equals("STBA")) {
                if (addressingMode.equals("d")) {
                    machineCode.add("F1");
                }
            } else if (instruction.equals("LDWA")) {
                if (addressingMode.equals("i")) {
                    machineCode.add("C1");
                } else if (addressingMode.equals("d")) {
                    machineCode.add("C2");
                }
            } else if (instruction.equals("STWA")) {
                if (addressingMode.equals("d")) {
                    machineCode.add("E1");
                }
            } else if (instruction.equals("ADDA")) {
                if (addressingMode.equals("i")) {
                    machineCode.add("70");
                } else if (addressingMode.equals("d")) {
                    machineCode.add("71");
                }
            } else if (instruction.equals("ANDA")) {
                if (addressingMode.equals("i")) {
                    machineCode.add("B0");
                } else if (addressingMode.equals("d")) {
                    machineCode.add("B1");
                }
            } else if (instruction.equals("CPBA")) {
                if (addressingMode.equals("i")) {
                    machineCode.add("A0");
                } else if (addressingMode.equals("d")) {
                    machineCode.add("A1");
                }
            } else if (instruction.equals("BRNE")) {
                if (addressingMode.equals("i")) {
                    machineCode.add("14");
                }
            }

            // Add operand bytes
            machineCode.add(operandHex.substring(0, 2));
            machineCode.add(operandHex.substring(2, 4));

            currentPosition += 3;
        }

        reader.close();
        return machineCode;
    }
}