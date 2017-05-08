import java.io.BufferedReader;
import java.io.BufferedWriter;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

/*
 * Clasa Main - threadul principal. Vom prelucra argumentele
 * date programului, vom porni generatorii, apoi vom citi din coada
 * de evenimente si le vom da workerilor. Dupa care, vom sorta 
 * rezultatele obtinute si le vom scrie in fisierele corespunzatoare.
 */

public class Main {
	
	static int sizeQueue;
	static int numberOfEvents;
	static int numberOfThreads;
	static int numberOfTasks;
	
	static ArrayBlockingQueue<Event> queue;
	
	static BufferedReader reader;
	static BufferedReader[] files;
	
	static MyThread[] threads;
	
	/* 
	 * Semafor prin care contorizam evenimentele adaugate in coada.
	 * Cand un generator adauga un eveniment in coada, elibereaza semaforul,
	 * iar inainte de a se lua un eveniment din coada se face acquire, pentru
	 * a ne asigura ca generatorii au reusit sa adauge evenimente in coada.
	 */
	static Semaphore semaphore = new Semaphore(0);
	
	/*
	 * Al doilea semafor este folosit pentru a ne asigura ca workerii au 
	 * terminat de procesat evenimentele, astfel threadul main va putea
	 * sorta rezultatele, fara a exista problema ca s-au mai adaugat noi
	 * rezultate in liste pe parcursul sortarii sau scrierii in fisier.
	 */
	static Semaphore semaphoreForEnd = new Semaphore(0);
	
	/*
	 * Bariera este folosita pentru a astepta toate threadurile sa termine
	 * citirea evenimentelor. Dupa aceasta avem inca un release la primul
	 * semafor, avand ca efect anuntarea threadului main ca in coada nu vor
	 * mai fi adaugate evenimente.
	 */
	static CyclicBarrier barrier;
	
	// Listele in care se vor salva rezultatele pentru evenimente
	static ArrayList<Integer> prime = new ArrayList<Integer>();
	static ArrayList<Integer> fact = new ArrayList<Integer>();
	static ArrayList<Integer> fib = new ArrayList<Integer>();
	static ArrayList<Integer> square = new ArrayList<Integer>();
	
	
	public static void main(String[] args) {
		
		// Prelucrarea datelor primite ca parametru
		sizeQueue = Integer.parseInt(args[0]);
		numberOfEvents = Integer.parseInt(args[1]);
		numberOfThreads = args.length - 2;
		numberOfTasks = numberOfEvents * numberOfThreads;
		
		// Bariera pentru cati generatori de evenimente avem
		barrier = new CyclicBarrier(numberOfThreads);
		
		
		files = new BufferedReader[numberOfThreads];
		threads = new MyThread[numberOfThreads];
		queue = new ArrayBlockingQueue<Event>(sizeQueue);
		
		/*
		 * Deschidem fisierele cu evenimente
		 */
		for (int i = 0; i < numberOfThreads; i++){
			try{
				files[i] = new BufferedReader(new FileReader(args[i+2]));
			}catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		/*
		 * Configuram generatorii (thread-urile) - adaugam fisierul
		 * din care vor citi si numarul de evenimente ce vor fi citite
		 */
		for (int i = 0; i < numberOfThreads; i++){
			threads[i] = new MyThread(files[i], numberOfEvents);
		}
		
		
		/*
		 * Pornim thread-urile (generatorii de evenimente)
		 */
		for (int i = 0; i < numberOfThreads; i++){
			threads[i].start();
		}

		/*
		 * Thread-ul main va astepta pana un thread generator va adauga un
		 * eveniment in coada de evenimente
		 */
		try{	
			semaphore.acquire();
		}catch (InterruptedException e) {
			e.printStackTrace();
		}

		/*
		 * Cream un worker pool folosind ExecutorService - avem atatia
		 * workeri cate core-uri are procesorul
		 */
		int cores = Runtime.getRuntime().availableProcessors();
		ExecutorService executor = Executors.newFixedThreadPool(cores);
		
		
		// Incepem sa scoatem evenimente din coada
		int nTasks = 0;
		while (queue.isEmpty() == false){
			
			Event event = new Event();
	
			try{
				// scoatem un eveniment din coada
				event = queue.take();
				nTasks++;
				
				// il trimitem unui worker din pool
				executor.submit(new Worker(executor, event, 0, 1, 1, nTasks));

				// Facem un acquire pe semafor pentru a ne asigura ca,
				// coada nu este goala. Altfel, ar iesi din while, chiar
				// daca mai sunt evenimente de generat
				semaphore.acquire();
					
	        }catch(InterruptedException e) {
	            e.printStackTrace();
	        }
		}

		
		// Asteptam executia tuturor task-urilor
		try{	
			semaphoreForEnd.acquire();
		}catch (InterruptedException e) {
			e.printStackTrace();
		}

		// Sortam listele de rezultate
		Collections.sort(prime);
		Collections.sort(fact);
		Collections.sort(fib);
		Collections.sort(square);
		
		/*
		 * Scriem rezultatele in fisierele corespunzatoare
		 * fiecarei liste
		 */
		
		// PRIME
		try {
			BufferedWriter filePrime = new BufferedWriter(new FileWriter("PRIME.out"));
			for (Integer p : prime){
				filePrime.write(new String(p + "\n"));
			}
			
			filePrime.close();
		
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// FACT
		try {
			BufferedWriter fileFact = new BufferedWriter(new FileWriter("FACT.out"));
			for (Integer f : fact){
				fileFact.write(new String(f + "\n"));
			}
		
			fileFact.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// SQUARE
		try {
			BufferedWriter fileSquare = new BufferedWriter(new FileWriter("SQUARE.out"));
			for (Integer s : square){
				fileSquare.write(new String(s + "\n"));
			}
			
			fileSquare.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// FIB
		try {
			BufferedWriter fileFib = new BufferedWriter(new FileWriter("FIB.out"));
			for (Integer f : fib){
				fileFib.write(new String(f + "\n"));
			}
			
			fileFib.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}	
}
