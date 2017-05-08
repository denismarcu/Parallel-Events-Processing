import java.io.BufferedReader;
import java.io.IOException;
import java.util.concurrent.BrokenBarrierException;

/*
 * Clasa MyThread implementeaza un generator de evenimente,
 * care va citi evenimente dintr-un fisier si le va pune
 * in coada de evenimente.
 */

public class MyThread extends Thread {
	BufferedReader file;
	int  numberOfEvents;
	int time;
	
	/*
	 * Constructorul va primi fisierul din care citeste si 
	 * numarul de evenimente ce vor fi citite din fisier
	 */
	public MyThread( BufferedReader file, int  numberOfEvents){
		this.file = file;
		this.numberOfEvents = numberOfEvents;
	}
	
	@Override
	public void run() {
		
		String line;	
		for (int i = 0; i < numberOfEvents; i++){
			
			/*
			 * Citim linie cu linie. Fiecare linie va fi despartita
			 * in cuvinte, astfel vom extrage timpul de asteptare
			 * si datele pentru un eveniment
			 */
			
			try {
				line = file.readLine();
				String[] words;
				words = line.split(",");
				
				// Extragem timpul de sleep pentru thread
				time = Integer.parseInt(words[0]);
				Thread.sleep(time);
				
				// Creem un eveniment cu datele citite
				Event event = new Event();
				event.N = Integer.parseInt(words[2]);
				event.type = Type.valueOf(words[1]);
				
				// Adaugam evenimentul in coada.			
				Main.queue.put(event);
				
				// Eliberam semaforul. Threadul main va trebui sa astepte
				// pana adaugam evenimente in coada, ca sa ne asiguram ca 
				// toate evenimentele generate vor fi prelucrate de workeri.
				synchronized (Main.semaphore){
					Main.semaphore.release();
				} 

			} catch (IOException e) {
				e.printStackTrace();			
			} 
			catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		/*
		 * Inchidem fisierul din care am citit evenimentele. Avem si o
		 * bariera pentru a astepta toate thread-urile sa termine de 
		 * generat evenimentele. Cand au ajuns toate in acest punct, 
		 * putem elibera si ultimul semafor, avand ca efect anuntarea
		 * threadului main ca in coada nu vor mai fi adaugate evenimente.
		 */
		try {
			file.close();
			
			Main.barrier.await();
			
			synchronized (Main.semaphore){
				Main.semaphore.release();
			} 
			
		} catch (IOException | InterruptedException | BrokenBarrierException e) {
			e.printStackTrace();
		}
	}
}
