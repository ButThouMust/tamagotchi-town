; Summary: Modify the text printing code to use half-width characters (8x16)
; instead of full-width characters (16x16).

check title "NP TAMAGOTCH TOWN    "

lorom
math pri on

; ------------------------------------------------------------------------------
; ------------------------------------------------------------------------------

; Change the main "print character" subroutine to set the top and bottom tile
; IDs (2x1) for an encoding value, instead of the top-left, top-right,
; bottom-left, and bottom-right tile IDs (2x2).

org $819A02
    sta ($61),y     ; store top tile's ID
    pha             ; 

    tya             ; go down 1 tile
    clc             ; 
    adc #$0040      ;  
    tay             ; 

    pla             ; store bottom tile's ID
    clc             ; 
    adc #$0010      ; 
    sta ($61),y     ; 

    tya             ; go up 1 tile and right 1 tile
    sec             ; i.e. char is printed, move one tile right of top
    sbc #$003e      ; 
    tay             ; 

    rts             ;

; Do the same thing but for the character printing routine for descriptions of
; Tamagotchis. Exact same assembly code.

org $81B168
    sta ($61),y     ; store top tile's ID
    pha             ; 

    tya             ; go down 1 tile
    clc             ; 
    adc #$0040      ;  
    tay             ; 

    pla             ; store bottom tile's ID
    clc             ; 
    adc #$0010      ; 
    sta ($61),y     ; 

    tya             ; go up 1 tile and right 1 tile
    sec             ; i.e. char is printed, move one tile right of top
    sbc #$003e      ; 
    tay             ; 

    rts             ;

; original ASM for your convenience; copied from Mesen's "Edit code" window
  ; STA ($61),Y   ; store top left tile's ID

  ; INY           ; go right 1 tile
  ; INY           ; 
  ; INC           ; store top right tile's ID (TL+1)
  ; STA ($61),Y   ; 
  ; PHA           ; 

  ; TYA           ; go down 1 tile and left 1 tile
  ; CLC           ; 
  ; ADC #$003E    ; 
  ; TAY           ; 

  ; PLA           ; store bottom left tile's ID (TL + 0x10)
  ; CLC           ; 
  ; ADC #$000F    ; 
  ; STA ($61),Y   ; 

  ; INY           ; go right 1 tile
  ; INY           ; 
  ; INC           ; store bottom right tile's ID (TL + 0x11)
  ; STA ($61),Y   ; 

  ; TYA           ; go up 1 tile and right 1 tile
  ; SEC           ; i.e. char is printed, move two tiles right of TL
  ; SBC #$003E    ; 
  ; TAY           ; 

  ; RTS           ; 

; ------------------------------------------------------------------------------
; ------------------------------------------------------------------------------

; Change the width of the space character to be 1 tile. Also applies to code
; snippets that accomplish "go right one character".

; for a space character
org $81965F
    iny
    iny
    nop     ; originally iny
    nop     ; originally iny

; for a space character when doing control code 0F
org $8197AC
    iny
    iny
    nop     ; originally iny
    nop     ; originally iny

; for printing a decimal digit
org $8198D9
    iny
    iny
    nop     ; originally iny
    nop     ; originally iny

; may be unused? changing it anyway
org $819900
    iny
    iny
    nop     ; originally iny
    nop     ; originally iny

; for printing a BCD digit
org $81998F
    iny
    iny
    nop     ; originally iny
    nop     ; originally iny

; ------------------------------------------------------------------------------
; ------------------------------------------------------------------------------

; if you need/want to change the character encoding for the space character
; from 0x0142 to something else, here is a list of locations where it is used

; org $81964F
; org $819795
; org $81B13C

; you likely will also want to change the encodings for the digits 0-9
; take their encoding values in your new table file, and:
; - OR them with 0x2000 to set high priority
; org $819A23
;     [fill in with encodings]
; - OR them with 0x2400 to set high priority and palette 1
; org $81B0F7
;     [fill in with encodings]

; also change the dash character's encoding if you like
; OR the new encoding with 0x2400 to set high priority and palette 1
; org $81B0B7
; org $81B0EB
