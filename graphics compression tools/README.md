# Recompressor: how to use
Prerequisites:
- Installation of Java on your machine
- The file(s) you want to compress, in this directory

---

1. Open a command line window (Powershell, WSL, Linux shell, etc.) in this directory.
2. Compile with `javac LZSSRecompress.java`.
3. Run using `java LZSSRecompress data1.bin [data2.bin data3.bin ...]`. You can fill in as many data files' names as you want.

# Decompressor: how to use
## Decompress graphics from the game ROM
Prerequisites:
- Installation of Java on your machine
- A ROM image of Tamagotchi Town in the root of this repository

---

Simply run the batch file `DUMP game graphics.bat` in the root directory.
If you are not on Windows, it should be simple to convert the batch file to a shell script for your operating system.

## Decompress graphics from a file
Prerequisites:
- Installation of Java on your machine
- The file(s) you want to decompress, in this directory

---

1. Open a command line window (Powershell, WSL, Linux shell, etc.) in this directory.
2. Compile with `javac TamagotchiTownGFXDecomp.java`.
3. Run using `java TamagotchiTownGFXDecomp data1.bin [data2.bin data3.bin ...]`. You can fill in as many data files' names as you want.
