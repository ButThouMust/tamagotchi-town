prompt $g

@set newROM=".\Tamagotch Town (half width hack).sfc"

copy /y ".\Tamagotch Town (Japan) (NP).sfc" %newROM%
asar.exe "tamagotchi town half width printing.asm" %newROM%
:: superfamicheck.exe %newROM% --fix --silent
