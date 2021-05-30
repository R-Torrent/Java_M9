/** Back-end Java, IT Academy
*** M9 - Milestone 3
*** Roger Torrent */

package M9_Milestone3;

import java.util.Scanner;

/*
 * Las clases herederas de 'Ignition' utilizan un 'ExecutorService' para administrar los hilos que simulan
 * la progresi�n de cada propulsor del cohete. Hay un hilo por propulsor, pero es el 'ExecutorService'
 * quien se encarga de asignar, en cada ciclo, las parejas hilo-propulsor. Cada iteraci�n del c�lculo se
 * detiene en una puerta ('CyclicBarrier') que no se abre hasta que es habilitada por un temporizador
 * 'Timer'. S�lo tras esta sincronizaci�n se validan los datos y se presentan ante el usuario. Adem�s,
 * antes de cada �maniobra de encendido� (Milestone 3), hay una cerradura de sincronizaci�n condicionada a
 * que el usario pulse el bot�n "Ignici�!". Finalmente, cada maniobra (Milestone 3) requiere de un nuevo
 * hilo que recorre las etapas (inici -> encesa -> seguiment) del encendido.
 * 
 * 
 * EXTRA: Aplicaci�n del patr�n de dise�o "Observer (Behavioral)" en las clases 'Ignition', sus herederas
 * 'Milestone2' y 'Milestone3', 'EventManager' y la interfaz 'EventListener'. ( El enunciado pone �Aplicar
 * patrons del disseny i best practices�. )
 * 
 * 
 * NOTA 1: La variable de control sobre el motor de un cohete es su gasto m�sico, que multiplicado por el
 * impulso espec�fico nos da el empuje del mismo. Conocer la potencia mec�nica desarrollada por el motor
 * es imprescindible para el estudio de sus actuaciones y evaluar las eficiencias energ�tica (combusti�n
 * qu�mica) y propulsiva (conversi�n en movimiento �til), pero NO es un dato necesario para integrar la
 * cinem�tica del movimiento.
 * 
 * 
 * NOTA 2: A falta de detalles, he supuesto que cada propulsor puede aumentar o disminuir su potencia en
 * 1 unidad / ciclo �aunque esta magnitud es regulable en Milestone 3�. Cada ciclo tiene una duraci�n de 2
 * segundos. Se ha ralentizado tanto para poder apreciar la evoluci�n del trabajo multihilo del programa;
 * en un cohete real (de combustible l�quido), la duraci�n TOTAL del proceso de encendido, desde que se da
 * la orden hasta que aqu�l desarrolla su potencia m�xima, es del orden de unos 5 segundos. Y durante los
 * primeros segundos, cuando se abren las v�lvulas y se activan las bombas, ni siquiera se genera potencia.
 * 
 * 
 * NOTA 3: La f�rmula presente en el enunciado "Milestone 2 - Fase d'algor�smia" NO FUNCIONA. La potencia
 * necesaria para cambiar la velocidad (en el intervalo de tiempo fijo impl�cito por la f�rmula y que se
 * supone muy superior a la duraci�n de esta simulaci�n de encendido de motores), volando en el espacio
 * exterior sin campos gravitatorios ni fricci�n, ES PROPORCIONAL a v�(v-vo), NO al (v-vo)� que saldr�a de
 * reordenar la f�rmula impresa. A resultas de este error, seg�n el enunciado resulta razonable esperar
 * que se necesite id�ntica potencia para acelerar de, digamos, 100 hasta 120 que para un �mismo� salto
 * cuantitativo de 300 hasta 320. Pero lo m�s grave es que la f�rmula sugiere que la aceleraci�n es m�s
 * �econ�mica� si se parte la subida en escalones m�s peque�os:
 *      Subida de vo = 0 a v = 1.000 en una etapa: PT = 100
 *      Misma subida en cuatro etapas:
 *          vo =   0 a v =   250: PT(1) = 6,25
 *          vo = 250 a v =   500: PT(2) = 6,25
 *          vo = 500 a v =   750: PT(3) = 6,25
 *          vo = 750 a v = 1.000: PT(4) = 6,25
 * Evidentemente, no puede ser que la suma de estas potencias parciales (PT = 25), nos aceleren hasta la
 * misma velocidad final, en el vac�o del espacio, �con un 75% de ahorro de combustible!
 * 
 * ( Un apunte m�s, que podr�a considerarse una simplificaci�n pero convendr�a conocer: Adem�s de la
 * duraci�n del impulso para obtener el salto de velocidad, tambi�n la masa del cohete viene impl�cita en
 * el "100" de la f�rmula del enunciado. Pero, por un lado, la masa es muy distinta en distintos modelos de
 * cohete. Y por el otro, la masa del cohete no es constante durante su operaci�n: Precisamente obtiene el
 * empuje de impeler, a gran velocidad, una fracci�n considerable de aqu�lla en forma de gases por las
 * toberas. Est� claro que la masa de un cohete no puede ser igual antes que despu�s de quemar combustible.
 * Pero adem�s, y esto es algo particular a cohetes y misiles, ni siquiera se puede despreciar el gasto
 * instant�neo al plantear sus ecuaciones din�micas. )
 */
public class Main {
	
	static Scanner sc = new Scanner(System.in);
	
	public static void main(String[] args) {
		try {
			Coet[] coets = {
				new Coet("32WESSDS", new int[]{10, 30, 80}),
				new Coet("LDSFJA32", new int[]{30, 40, 50, 50, 30, 10}),
				new Coet("TORRENT1", new int[]{25, 125, 125, 125, 25}),
				// Nuevos modelos de cohete se a�aden aqu�
			};
			
			int op = 0;
			
			System.out.println("Esculli el seu coet:");
			for(int i = 1; i <= coets.length; i++)
				System.out.println(i + "> " + coets[i-1].printCoet());
			System.out.printf("? ");
			
			try {
				op = sc.nextInt();
				if(op < 1 || op > coets.length) throw new RuntimeException();
			} catch (RuntimeException e) { throw new Exception("Error: Coet inexistent!"); }
			Coet c = coets[--op];
			
			System.out.println("Esculli el mode d'operaci�:");
			System.out.println("1> Pot�ncia generada lliure (Milestone 1)");
			System.out.println("2> Programa d'acceleraci� fixat (Milestone 2)");
			System.out.println("3> Acceleraci� controlada - Interf�cie gr�fica (Milestone 3)");
			System.out.printf("? ");
			
			Ignition m;
			try {
				op = sc.nextInt();
				switch(op) {
				case 1: m = new Milestone1(c); break; // Carrega la missi� Milestone 1
				case 2: m = new Milestone2(c); break; // Carrega la missi� Milestone 2
				case 3: m = new Milestone3(c); break; // Carrega la missi� Milestone 3
				default: throw new RuntimeException();
				}
			} catch (RuntimeException e) { throw new Exception("Error: Mode desconegut!"); }
			
			m.maniobra(true); // Comen�a la simulaci� de l'encesa del coet
		} catch (Exception e) {
			System.out.println(e.getMessage());
			System.exit(128); // Dins UNIX, 128 �s un codi de sortida reservat: "Invalid argument to exit"
		} finally {	sc.close();	}
	}
	
}
