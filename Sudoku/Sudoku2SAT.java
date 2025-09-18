import java.io.*;
import java.util.ArrayList;
import java.util.List;
// Afficher le chemin actuel pour vérifier le répertoire de travail
public class Sudoku2SAT {
    private static List<List<Integer>> clauses;
    private static int[][] sudoku;
    private static int[][][] correspondant;
    private static boolean isBonne_taille = true;

    public static void main(String[] args) throws Exception {
        // Exemple de résolution pour une seule grille avec création d'un seul fichier CNF
        String sudokuStr = ".................1.....2.3......3.2...1.4......5....6..3......4.7..8...962...7...";
        int[][] sudoku = parseSudokuString(sudokuStr);
        afficherSudoku(sudoku); // Affiche la grille initiale
        String cnfFileName = "Sudoku/SAT/sudoku_single.cnf"; // Un seul fichier CNF dans le répertoire courant
        genererCNF(sudoku, cnfFileName); // Génère le fichier CNF

        // Appel à MiniSat pour résoudre le problème SAT
        boolean isSat = appelerMiniSat(cnfFileName);
        if (isSat) {
            System.out.println("Solution trouvée:");
            afficherSudoku(sudoku); // Affiche la grille Sudoku résolue
        } else {
            System.out.println("Pas de solution (UNSAT)");
        }


        // Exemple de mesure de performance pour les benchmarks avec des chemins relatifs
        String benchmarkFile1 = "all_sudoku/benchmark_sudoku_1.txt";
        String benchmarkFile2 = "all_sudoku/benchmark_sudoku_2.txt";
        System.out.println("\nMesure de performance pour " + benchmarkFile1);
        mesurerPerformance(benchmarkFile1);
        System.out.println("\nMesure de performance pour " + benchmarkFile2);
        mesurerPerformance(benchmarkFile2);
    }

    // Convertit une chaîne de caractères représentant un Sudoku en tableau 2D d'entiers
    static int[][] parseSudokuString(String sudokuStr) {
        int[][] board = new int[9][9];
        int length = sudokuStr.length();

        if (length != 81) {
            throw new IllegalArgumentException("La chaîne Sudoku doit contenir 81 caractères.");
        }

        int idx = 0;
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                char ch = sudokuStr.charAt(idx++);
                if (ch == '.') {
                    board[i][j] = 0; // Les points représentent les cases vides
                } else if (Character.isDigit(ch)) {
                    board[i][j] = Character.getNumericValue(ch); // Conversion des chiffres en entiers
                } else {
                    throw new IllegalArgumentException("Caractère invalide dans la chaîne Sudoku.");
                }
            }
        }
        return board;
    }

    // Affiche la grille Sudoku
    private static void afficherSudoku(int[][] board) {
        for (int[] row : board) {
            for (int cell : row) {
                System.out.print((cell == 0 ? ". " : cell + " "));
            }
            System.out.println();
        }
        System.out.println();
    }

    // Méthode pour générer un fichier CNF pour une grille donnée
    static void genererCNF(int[][] data, String cnfFileName) throws IOException {
        sudoku = data;
        clauses = new ArrayList<>();
        if (isBonne_taille) {
            initValeurCorrespondant(); // Initialisation du mapping des littéraux
        }

        manipulationUneParLigne();
        manipulationUneParColonne();

        for (int i = 0; i < sudoku.length; i++) {
            for (int j = 0; j < sudoku.length; j++) {
                if (sudoku[i][j] != 0) {
                    manipulationNumeroExistant(sudoku[i][j], i + 1, j + 1);
                } else {
                    manipulationAuMoinUnNuméro(i + 1, j + 1);
                    manipulationAuMaxUnNuméro(i + 1, j + 1);
                }
            }
        }

        int regionSize = (int) Math.sqrt(sudoku.length);
        for (int i = 0; i < sudoku.length; i += regionSize) {
            for (int j = 0; j < sudoku.length; j += regionSize) {
                manipulationDesRegions(i + 1, j + 1, regionSize);
            }
        }

        // Écriture dans un fichier unique
        ecritureCNF(cnfFileName);
    }

    // Appeler MiniSat et récupérer la solution
    static boolean appelerMiniSat(String cnfFileName) throws IOException, InterruptedException {
        String minisatOutput = "output.txt";
        ProcessBuilder pb = new ProcessBuilder("minisat", cnfFileName, minisatOutput);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        process.waitFor();

        // Lire le fichier de sortie de MiniSat
        BufferedReader reader = new BufferedReader(new FileReader(minisatOutput));
        String line = reader.readLine();
        if (line != null && line.equals("SAT")) {
            // Si satisfiable, lire les assignations
            while ((line = reader.readLine()) != null) {
                String[] assignments = line.split(" ");
                for (String lit : assignments) {
                    int literal = Integer.parseInt(lit);
                    if (literal > 0) {
                        // Si un littéral est vrai, on assigne le numéro correspondant dans la grille Sudoku
                        assignerValeurSudoku(literal);
                    }
                }
            }
            return true;
        } else {
            return false; // UNSAT
        }
    }

    // Assigner une valeur dans la grille Sudoku à partir d'un littéral
    private static void assignerValeurSudoku(int literal) {
        for (int i = 0; i < sudoku.length; i++) {
            for (int j = 0; j < sudoku.length; j++) {
                for (int k = 0; k < sudoku.length; k++) {
                    if (correspondant[i][j][k] == literal) {
                        sudoku[i][j] = k + 1; // On assigne la valeur k+1 à la case
                    }
                }
            }
        }
    }

    // Initialisation du tableau de correspondance des littéraux
    private static void initValeurCorrespondant() {
        correspondant = new int[sudoku.length][sudoku.length][sudoku.length];
        int value = 1;
        for (int i = 0; i < sudoku.length; i++) {
            for (int j = 0; j < sudoku.length; j++) {
                for (int k = 0; k < sudoku.length; k++) {
                    correspondant[i][j][k] = value++;
                }
            }
        }
    }

    // Ajoute des clauses pour garantir qu'il y a au moins un nombre par ligne
    private static void manipulationUneParLigne() {
        for (int i = 1; i <= sudoku.length; i++) {
            for (int k = 1; k <= sudoku.length; k++) {
                List<Integer> c = new ArrayList<>();
                for (int j = 1; j <= sudoku.length; j++) {
                    c.add(getLitteral(k, i, j));
                }
                clauses.add(c);
            }
        }
    }

    // Ajoute des clauses pour garantir qu'il y a au moins un nombre par colonne
    private static void manipulationUneParColonne() {
        for (int j = 1; j <= sudoku.length; j++) {
            for (int k = 1; k <= sudoku.length; k++) {
                List<Integer> c = new ArrayList<>();
                for (int i = 1; i <= sudoku.length; i++) {
                    c.add(getLitteral(k, i, j));
                }
                clauses.add(c);
            }
        }
    }

    private static void manipulationNumeroExistant(int k, int i, int j) {
        List<Integer> c = new ArrayList<>();
        c.add(getLitteral(k, i, j));
        clauses.add(c);
    }

    private static void manipulationAuMoinUnNuméro(int i, int j) {
        List<Integer> c = new ArrayList<>();
        for (int k = 1; k <= sudoku.length; k++) {
            c.add(getLitteral(k, i, j));
        }
        clauses.add(c);
    }

    private static void manipulationAuMaxUnNuméro(int i, int j) {
        for (int k = 1; k <= sudoku.length; k++) {
            for (int kprime = k + 1; kprime <= sudoku.length; kprime++) {
                List<Integer> c = new ArrayList<>();
                c.add(-getLitteral(k, i, j));
                c.add(-getLitteral(kprime, i, j));
                clauses.add(c);
            }
        }
    }

    // Gère les contraintes pour chaque région 3x3 du Sudoku
    private static void manipulationDesRegions(int startRow, int startCol, int regionSize) {
        for (int k = 1; k <= 9; k++) {
            for (int i1 = 0; i1 < regionSize; i1++) {
                for (int j1 = 0; j1 < regionSize; j1++) {
                    for (int i2 = 0; i2 < regionSize; i2++) {
                        for (int j2 = 0; j2 < regionSize; j2++) {
                            if (i1 == i2 && j1 == j2) continue;
                            List<Integer> c = new ArrayList<>();
                            c.add(-getLitteral(k, startRow + i1, startCol + j1));
                            c.add(-getLitteral(k, startRow + i2, startCol + j2));
                            clauses.add(c);
                        }
                    }
                }
            }
        }
    }

    // Renvoie le littéral correspondant à un nombre k dans la cellule (i, j)
    private static int getLitteral(int k, int i, int j) {
        return correspondant[i - 1][j - 1][k - 1];
    }

    private static void ecritureCNF(String name) throws IOException {
        // Print the path for debugging
        //System.out.println("Writing CNF file to: " + name);

        File outFile = new File(name);

        // Ensure the parent directory exists
        if (outFile.getParentFile() != null && !outFile.getParentFile().exists()) {
            if (!outFile.getParentFile().mkdirs()) {
                System.err.println("Failed to create directories for path: " + name);
                return; // Exit if directory creation fails
            }
        }

        // Create the file writer to write the CNF file
        FileWriter myWriter = new FileWriter(outFile);
        int nbLiterals = sudoku.length * sudoku.length * sudoku.length;
        myWriter.write("p cnf " + nbLiterals + " " + clauses.size() + "\n");

        for (List<Integer> clause : clauses) {
            for (int lit : clause) {
                myWriter.write(lit + " ");
            }
            myWriter.write("0\n");
        }

        myWriter.close();
    }


    static void mesurerPerformance(String benchmarkFile) throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader(benchmarkFile));
        String line;
        long startTime = System.currentTimeMillis();
        int solvedCount = 0;

        while ((line = reader.readLine()) != null) {
            if (System.currentTimeMillis() - startTime > 1000) {
                break; // Arrête après 1 seconde
            }

            int[][] sudoku = parseSudokuString(line);

            // Ensure you pass a valid file name
            String cnfFilePath = "performance_output_" + solvedCount + ".cnf";
            genererCNF(sudoku, cnfFilePath); // Generate CNF and save to file

            boolean isSat = appelerMiniSat(cnfFilePath);
            if (isSat) {
                solvedCount++;
            }

            // Optionally delete the CNF file after processing to avoid clutter
            new File(cnfFilePath).delete();
        }

        System.out.println("Nombre de grilles résolues en 1 seconde : " + solvedCount);
    }

}
