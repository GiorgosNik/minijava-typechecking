import syntaxtree.*;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.FileWriter;   

import src.VisitorPhase1;
import src.VisitorPhase2;
import src.VisitorPhase3;

public class Main {
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            //If the input is empty, print usage instruction
            System.err.println("Usage: java Main <inputFile>");
            System.exit(1);
        }
        int passed = 0;
        int failed = 0;
        String[] outFile;
        
        // For every file in input
        for (int i = 0; i < args.length; i++) {
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(args[i]);
                outFile = args[i].split(".java");
                System.out.println("Program: "+args[i]);
                MiniJavaParser parser = new MiniJavaParser(fis);

                Goal root = parser.Goal();

                System.err.println("Program parsed successfully.");

                // First Visitor will perform initial checking and create symbol table
                VisitorPhase1 eval = new VisitorPhase1();
                root.accept(eval, null);

                // Second Visitor will perform most of the type checking, using the symbol table from Visitor #1
                VisitorPhase2 checker = new VisitorPhase2();
                checker.passSymbolTable(eval.classes);
                root.accept(checker, null);

                VisitorPhase3 llvmComp = new VisitorPhase3();
                llvmComp.passSymbolTable(eval.classes);
                root.accept(llvmComp, null);
                try {
                    FileWriter myWriter = new FileWriter(outFile[0]+".ll");
                    myWriter.write(llvmComp.codeCollection);
                    myWriter.close();
                    System.out.println("Successfully wrote to the file.");
                  } catch (IOException e) {
                    System.out.println("An error occurred.");
                    e.printStackTrace();
                  }
                // The program was checked
                passed++;

            // Any Exceptions will lead to these 'catch' statements, count as a failure
            } catch (ParseException ex) {
                failed++;
                System.err.println(ex.getMessage());
            } catch (FileNotFoundException ex) {
                failed++;
                System.err.println(ex.getMessage());
            } catch (Exception ex) {
                failed++;
                System.err.println(ex.getMessage());
                    
            } finally {
                // Try to close the file
                try {
                    if (fis != null)
                        fis.close();
                } catch (IOException ex) {
                    System.err.println(ex.getMessage());
                }
            }
        }
        // Print the results
        System.out.println("Out of "+args.length +" programs, "+passed+" passed "+failed+" failed");

    }
}
