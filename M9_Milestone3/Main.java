/** Back-end Java, IT Academy
*** M9 - Milestone 3
*** Roger Torrent */

package M9_Milestone3;

import java.util.Scanner;

/*
 * Las clases herederas de 'Ignition' utilizan un 'ExecutorService' para administrar los hilos que simulan
 * la progresión de cada propulsor del cohete. Hay un hilo por propulsor, pero es el 'ExecutorService'
 * quien se encarga de asignar, en cada ciclo, las parejas hilo-propulsor. Cada iteración del cálculo se
 * detiene en una puerta ('CyclicBarrier') que no se abre hasta que es habilitada por un temporizador
 * 'Timer'. Sólo tras esta sincronización se validan los datos y se presentan ante el usuario. Además,
 * antes de cada «maniobra de encendido» (Milestone 3), hay una cerradura de sincronización condicionada a
 * que el usario pulse el botón "Ignició!". Finalmente, cada maniobra (Milestone 3) requiere de un nuevo
 * hilo que recorre las etapas (inici -> encesa -> seguiment) del encendido.
 * 
 * 
 * EXTRA: Aplicación del patrón de diseño "Observer (Behavioral)" en las clases 'Ignition', sus herederas
 * 'Milestone2' y 'Milestone3', 'EventManager' y la interfaz 'EventListener'. ( El enunciado pone «Aplicar
 * patrons del disseny i best practices». )
 * 
 * 
 * NOTA 1: La variable de control sobre el motor de un cohete es su gasto másico, que multiplicado por el
 * impulso específico nos da el empuje del mismo. Conocer la potencia mecánica desarrollada por el motor
 * es imprescindible para el estudio de sus actuaciones y evaluar las eficiencias energética (combustión
 * química) y propulsiva (conversión en movimiento útil), pero NO es un dato necesario para integrar la
 * cinemática del movimiento.
 * 
 * 
 * NOTA 2: A falta de detalles, he supuesto que cada propulsor puede aumentar o disminuir su potencia en
 * 1 unidad / ciclo —aunque esta magnitud es regulable en Milestone 3—. Cada ciclo tiene una duración de 2
 * segundos. Se ha ralentizado tanto para poder apreciar la evolución del trabajo multihilo del programa;
 * en un cohete real (de combustible líquido), la duración TOTAL del proceso de encendido, desde que se da
 * la orden hasta que aquél desarrolla su potencia máxima, es del orden de unos 5 segundos. Y durante los
 * primeros segundos, cuando se abren las válvulas y se activan las bombas, ni siquiera se genera potencia.
 * 
 * 
 * NOTA 3: La fórmula presente en el enunciado "Milestone 2 - Fase d'algorísmia" NO FUNCIONA. La potencia
 * necesaria para cambiar la velocidad (en el intervalo de tiempo fijo implícito por la fórmula y que se
 * supone muy superior a la duración de esta simulación de encendido de motores), volando en el espacio
 * exterior sin campos gravitatorios ni fricción, ES PROPORCIONAL a v·(v-vo), NO al (v-vo)² que saldría de
 * reordenar la fórmula impresa. A resultas de este error, según el enunciado resulta razonable esperar
 * que se necesite idéntica potencia para acelerar de, digamos, 100 hasta 120 que para un «mismo» salto
 * cuantitativo de 300 hasta 320. Pero lo más grave es que la fórmula sugiere que la aceleración es más
 * «económica» si se parte la subida en escalones más pequeños:
 *      Subida de vo = 0 a v = 1.000 en una etapa: PT = 100
 *      Misma subida en cuatro etapas:
 *          vo =   0 a v =   250: PT(1) = 6,25
 *          vo = 250 a v =   500: PT(2) = 6,25
 *          vo = 500 a v =   750: PT(3) = 6,25
 *          vo = 750 a v = 1.000: PT(4) = 6,25
 * Evidentemente, no puede ser que la suma de estas potencias parciales (PT = 25), nos aceleren hasta la
 * misma velocidad final, en el vacío del espacio, ¡con un 75% de ahorro de combustible!
 * 
 * ( Un apunte más, que podría considerarse una simplificación pero convendría conocer: Además de la
 * duración del impulso para obtener el salto de velocidad, también la masa del cohete viene implícita en
 * el "100" de la fórmula del enunciado. Pero, por un lado, la masa es muy distinta en distintos modelos de
 * cohete. Y por el otro, la masa del cohete no es constante durante su operación: Precisamente obtiene el
 * empuje de impeler, a gran velocidad, una fracción considerable de aquélla en forma de gases por las
 * toberas. Está claro que la masa de un cohete no puede ser igual antes que después de quemar combustible.
 * Pero además, y esto es algo particular a cohetes y misiles, ni siquiera se puede despreciar el gasto
 * instantáneo al plantear sus ecuaciones dinámicas. )
 */
public class Main {
	
	static Scanner sc = new Scanner(System.in);
	
	public static void main(String[] args) {
		try {
			Coet[] coets = {
				new Coet("32WESSDS", new int[]{10, 30, 80}),
				new Coet("LDSFJA32", new int[]{30, 40, 50, 50, 30, 10}),
				new Coet("TORRENT1", new int[]{25, 125, 125, 125, 25}),
				// Nuevos modelos de cohete se añaden aquí
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
			
			System.out.println("Esculli el mode d'operació:");
			System.out.println("1> Potència generada lliure (Milestone 1)");
			System.out.println("2> Programa d'acceleració fixat (Milestone 2)");
			System.out.println("3> Acceleració controlada - Interfície gràfica (Milestone 3)");
			System.out.printf("? ");
			
			Ignition m;
			try {
				op = sc.nextInt();
				switch(op) {
				case 1: m = new Milestone1(c); break; // Carrega la missió Milestone 1
				case 2: m = new Milestone2(c); break; // Carrega la missió Milestone 2
				case 3: m = new Milestone3(c); break; // Carrega la missió Milestone 3
				default: throw new RuntimeException();
				}
			} catch (RuntimeException e) { throw new Exception("Error: Mode desconegut!"); }
			
			m.maniobra(true); // Comença la simulació de l'encesa del coet
		} catch (Exception e) {
			System.out.println(e.getMessage());
			System.exit(128); // Dins UNIX, 128 és un codi de sortida reservat: "Invalid argument to exit"
		} finally {	sc.close();	}
	}
	
}
