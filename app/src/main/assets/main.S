.section .text
.global _start

_start:
    adr x1, message
    mov x2, #message_len
    mov x0, #1
    mov x8, #64
    svc #0

    mov x0, #0
    mov x8, #93
    svc #0

.section .rodata
message:
    .ascii "APDNOS: ELF online!\n"
message_len = . - message
