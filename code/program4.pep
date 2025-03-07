                 LDBA 0x0041, i 
next_let:        STBA 0xFC16, d
                 ADDA 0x0001, i
                 CPBA 0x005B, i
                 BRNE next_let, i

                 STOP

.END