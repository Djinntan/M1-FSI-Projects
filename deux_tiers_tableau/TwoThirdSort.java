import java.util.Arrays;
import java.util.Random;


public class TwoThirdSort {

    // Méthode principale pour le tri deux-tiers
    public static void twoThirdSort(int[] arr, int start, int end, int depth) {
        // Affiche l'état actuel du tableau à chaque appel de la méthode
        //System.out.println("Étape " + depth + " : " + Arrays.toString(Arrays.copyOfRange(arr, start, end)));

        if (end - start <= 0) {
            // Cas de base, rien à faire si un seul élément
            return;
        } else if (end - start == 1) {
            // Si deux éléments, les trier
            if (arr[start] > arr[start + 1]) {
                int temp = arr[start];
                arr[start] = arr[start + 1];
                arr[start + 1] = temp;
            }
            return;
        }
        int twoThird = (int) Math.ceil((2 * ((double) end - (double) start + 1)) / 3); // Calculer 2/3 de la section
      //  System.out.println(start + " " + end + " " + twoThird);

        // Étape (a) : trier les premiers 2/3 du tableau
        //System.out.println("Tri des premiers 2/3 : " + Arrays.toString(Arrays.copyOfRange(arr, start, start + twoThird)));
        twoThirdSort(arr, start, start + twoThird - 1, depth + 1);

        // Étape (b) : trier les derniers 2/3 du tableau
        // System.out.println("Tri des derniers 2/3 : " + Arrays.toString(Arrays.copyOfRange(arr, end - twoThird, end)));
        twoThirdSort(arr, end - twoThird + 1, end, depth + 1);

        // Étape (c) : re-trier les premiers 2/3 du tableau
        // System.out.println("Re-tri des premiers 2/3 : " + Arrays.toString(Arrays.copyOfRange(arr, start, start + twoThird)));
        twoThirdSort(arr, start, start + twoThird - 1, depth + 1);

        // Fusionner la sous-section triée pour garantir que tout soit bien en ordre
        //Arrays.sort(arr, start, end); // Cela permet de s'assurer que l'ensemble des 2 parties est fusionné correctement
    }

    // Méthode pour générer un tableau aléatoire de taille donnée
    public static int[] generateRandomArray(int size) {
        Random rand = new Random();
        int[] arr = new int[size];
        for (int i = 0; i < size; i++) {
            arr[i] = rand.nextInt(100); // Générer des nombres entre 0 et 100 pour faciliter la lecture
        }
        return arr;
    }

    // Test de l'algorithme
    public static void main(String[] args) {
        int[] arr = generateRandomArray(31); // Générer un tableau aléatoire de taille 6 pour le test
        System.out.println("Avant le tri: " + Arrays.toString(arr));
        twoThirdSort(arr, 0, arr.length - 1, 0); // Appel au tri deux-tiers
        System.out.println("Après le tri: " + Arrays.toString(arr));
    }
}
