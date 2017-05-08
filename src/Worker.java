import java.util.concurrent.ExecutorService;

/*
 * Clasa Worker implementeaza un worker din pool
 */
public class Worker implements Runnable {
	ExecutorService tpe;
	Event event;
	int number0;
	int number1;
	int result;
	int nTasks;
	
	public Worker(ExecutorService tpe, Event event, int number0, int number1, int result, int nTasks){
		this.tpe = tpe;
		this.event = event;
		this.number0 = number0;
		this.number1 = number1;
		this.result = result;
		this.nTasks = nTasks;
	}

	/*
	 * Functie care verifica daca un numar din sirul fibonacci
	 * este mai mare decat numarul specific evenimentului
	 */
	boolean isFiboGreater(int fib_0, int fib_1){
		if (fib_0 + fib_1 > this.event.N){
			return true;
		}
		return false;
	}
	
	/*
	 * Functie care verifica daca un factorial este mai mare
	 * decat numarul specific evenimentului
	 */
	boolean isFactorialGreater(int factorial){
		if (factorial > event.N){
			return true;
		}
		return false;
	}
	
	/*
	 * Functie care verifica daca un numar este prim
	 */
	boolean isPrime(int n){
		if (n % 2 == 0 && n != 2){
			return false;
		}
		if (n == 2 || n == 3){
			return true;
		}
		
		int radical = (int) Math.sqrt(n);
		for(int i = 2; i <= radical; i++){
			if (n % i == 0){
				return false;
			}
		}
		return true;
	}
	
	/*
	 * Functie care verifica daca patratul unui numar
	 * este mai mare decat numarul specific evenimentului
	 */
	boolean isSquareGreater(int number){
		if (number * number > event.N){
			return true;
		}
		return false;
	}
	
	
	@Override
	public void run() {
		
		/*
		 * Evenimentul este de tip FIB - calculam cel mai mare numar pentru
		 * care valoarea corespunzatoare din sirul fibonacci este mai
		 * mica sau egala cu N. 
		 * Daca l-am gasit, atunci il vom adauga in lista de rezultate
		 * pentru FIB (se va sincroniza lista), iar daca nu, vom crea
		 * un nou task, ce va verifica daca pentru numarul urmator, valoarea
		 * corespunzatoare din sirul lui fib este mai mica decat N.
		 */
		if (event.type == Type.FIB){
			
			if(isFiboGreater(number0, number1) == true){

				// Adaugam rezultatul in lista sincronizata
				synchronized(Main.fib){
					Main.fib.add(result);
				}
				
				// Daca acesta a fost ultimul eveniment, atunci
				// eliberam semaforul pentru sortarea rezultatelor
				// si inchidem task-urile
				if (nTasks == Main.numberOfTasks){
					int size = Main.fib.size() + Main.fact.size();
					size += Main.prime.size() + Main.square.size();
					
					// Adaugam bucla aceasta pentru a ne asigura ca
					// toate celelalte task-uri s-au incheiat, adica
					// niciun task nu mai are de adaugat rezultate
					while (size != nTasks){
						size = Main.fib.size() + Main.fact.size();
						size += Main.prime.size() + Main.square.size();
					}
					
					// Eliberam semaforul si inchidem taskurile
					Main.semaphoreForEnd.release();
					tpe.shutdown();
				}
				
			}
			else{
				// calculam urmatorul fibonacci
				int fibonacci = number0 + number1;
				number0 = number1;
				number1 = fibonacci;
				result++;
				tpe.submit(new Worker(tpe, event, number0, number1, result, nTasks));
			}
			
		}
		
		
		/*
		 * Evenimentul este de tip FACT - cautam cel mai mare numar 
		 * care are factorialul mai mic sau egal cu N.
		 * Daca l-am gasit, atunci il vom adauga in lista de rezultate
		 * pentru FACT (se va sincroniza lista), iar daca nu, vom crea
		 * un nou task, ce va verifica daca urmatorul numar are factorialul
		 * mai mic sau egal cu N
		 */
		else if (event.type == Type.FACT){
			
			if(isFactorialGreater(number1) == true){

				// Adaugam rezultatul in lista sincronizata
				result--;
				synchronized(Main.fact){
					Main.fact.add(result);
				}

				// Daca acesta a fost ultimul eveniment, atunci
				// eliberam semaforul pentru sortarea rezultatelor
				// si inchidem task-urile
				if (nTasks == Main.numberOfTasks){
					
					int size = Main.fib.size() + Main.fact.size();
					size += Main.prime.size() + Main.square.size();
					
					// Adaugam bucla aceasta pentru a ne asigura ca
					// toate celelalte task-uri s-au incheiat, adica
					// niciun task nu mai are de adaugat rezultate
					while (size != nTasks){
						size = Main.fib.size() + Main.fact.size();
						size += Main.prime.size() + Main.square.size();
					}
					
					// Eliberam semaforul si inchidem taskurile
					Main.semaphoreForEnd.release();
					tpe.shutdown();
				}
			}
			else{
				// Apelam un alt task pentru urmatorul numar
				result ++;
				number1 *= result;
				tpe.submit(new Worker(tpe, event, number0, number1, result, nTasks));
			}
		}
		
		
		/*
		 * Evenimentul este de tip PRIME - calculam cel mai mare numar
		 * prim mai mic sau egal cu N.
		 * Daca l-am gasit, atunci il vom adauga in lista de rezultate
		 * pentru PRIME (se va sincroniza lista), iar daca nu, vom crea
		 * un nou task, ce va verifica daca urmatorul numar impar mai mic 
		 * decat cel curent este prim.
		 */
		else if (event.type == Type.PRIME){
			
			// N este par, deci vom pleca de la N-1
			if (event.N % 2 == 0 && event.N != 2){
				event.N--;
			}
		
			if(isPrime(event.N) == true){
				
				// Adaugam rezultatul in lista sincronizata
				synchronized(Main.prime){
					Main.prime.add(event.N);
				}

				// Daca acesta a fost ultimul eveniment, atunci
				// eliberam semaforul pentru sortarea rezultatelor
				// si inchidem task-urile
				if (nTasks == Main.numberOfTasks){
					
					int size = Main.fib.size() + Main.fact.size();
					size += Main.prime.size() + Main.square.size();
					
					// Adaugam bucla aceasta pentru a ne asigura ca
					// toate celelalte task-uri s-au incheiat, adica
					// niciun task nu mai are de adaugat rezultate
					while (size != nTasks){
						size = Main.fib.size() + Main.fact.size();
						size += Main.prime.size() + Main.square.size();
					}
					
					// Eliberam semaforul si inchidem taskurile
					Main.semaphoreForEnd.release();
					tpe.shutdown();
				}
			}
			else{
				// Apelam un alt task pentru urmatorul numar impar
				// mai mic decat cel curent
				event.N -= 2;
				tpe.submit(new Worker(tpe, event, number0, number1, result, nTasks));
			}
		}
		
		
		/*
		 * Evenimentul este de tip SQUARE - calculam cel mai mare numar
		 * care are patratul mai mic sau egal cu N.
		 * Daca l-am gasit, atunci il vom adauga in lista de rezultate
		 * pentru SQUARE (se va sincroniza lista), iar daca nu, vom crea
		 * un nou task, ce va verifica daca numarul urmator este cel
		 * cautat.
		 */
		else if (event.type == Type.SQUARE){
			
			if(isSquareGreater(result) == true){
				
				// Adaugam rezultatul in lista sincronizata
				result--;
				synchronized(Main.square){
					Main.square.add(result);
				}

				// Daca acesta a fost ultimul eveniment, atunci
				// eliberam semaforul pentru sortarea rezultatelor
				// si inchidem task-urile
				if (nTasks == Main.numberOfTasks){
					
					int size = Main.fib.size() + Main.fact.size();
					size += Main.prime.size() + Main.square.size();
					
					// Adaugam bucla aceasta pentru a ne asigura ca
					// toate celelalte task-uri s-au incheiat, adica
					// niciun task nu mai are de adaugat rezultate
					while (size != nTasks){
						size = Main.fib.size() + Main.fact.size();
						size += Main.prime.size() + Main.square.size();
					}
					
					// Eliberam semaforul si inchidem taskurile
					Main.semaphoreForEnd.release();
					tpe.shutdown();
				}
			}
			else{
				// Apelam un alt task pentru urmatorul numar 
				result ++;
				tpe.submit(new Worker(tpe, event, number0, number1, result, nTasks));
			}
		}
	}
}
