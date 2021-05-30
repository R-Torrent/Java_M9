package M9_Milestone3;

import java.util.StringJoiner;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

/*
 * NOTA: Estas clases se escribieron contemplando la posibilidad de operar varios cohetes a la vez. Es
 * por ello que las variables de potencia total no son estáticas adscritas a la clase 'Propulsor', como
 * sería más natural manejando un solo cohete.
 */
public class Coet {
	
	String codi;
	Propulsor[] planta;
	CyclicBarrier fiDeCicle; // Barrera que s'aixeca quan tots el propulsors acaben les seves tasques
	
	MutableInt maxPTotal, actPTotal, dstPTotal; // Potències màxima, actual i desitjada totals del coet
	MutableInt prvPTotal; // Potència total en l'instant previ
	int widthTotal; // Nombre de dígits necessaris per escriure la potència total del coet
	
	Coet(String codi, int[] maxPs) {
		this.codi = codi;
		planta = new Propulsor[maxPs.length];
		fiDeCicle = new CyclicBarrier(planta.length + 1);
		
		maxPTotal = new MutableInt(0);
		for(int i = 0; i < maxPs.length; i++) {
			planta[i] = new Propulsor(maxPs[i], i, fiDeCicle);
			maxPTotal.value += maxPs[i];
		}
		actPTotal = new MutableInt(0);
		dstPTotal = new MutableInt(0);
		prvPTotal = new MutableInt(0);
		widthTotal = (int)Math.log10(maxPTotal.value) + 1;
	}
	
	String printCoet() {
		StringJoiner textMaxPs = new StringJoiner(", ");
		for(Propulsor p : planta)
			textMaxPs.add(p.maxP.toString());
		
		return codi + ", " + planta.length + " propulsors (pot. màxima): " + textMaxPs.toString();
	}
	
	void reset() { // Limpieza al empezar una nueva misión
		actPTotal.value = 0;
		dstPTotal.value = 0;
		prvPTotal.value = 0;
		for(Propulsor p : planta) p.reset();
	}
	
}

/*
 * Cada uno de los propulsores calcula la progresión de su potencia de manera independiente
 * en un 'thread' particular.
 */
class Propulsor implements Runnable {
	
	static Integer[] passos = {1, 5, 10, 50, 100}; // Pasos por ciclo de los propulsores
	
	MutableInt maxP, actP, dstP; // Potències màxima, actual i desitjada del propulsor
	MutableInt prvP; // Potència actual en l'instant previ
	int width; // Nombre de dígits necessaris per escriure la potència del propulsor
	int idP; // Número de identificació del propulsor (= índex)
	CyclicBarrier fiDeCicle; // Tanca comuna
	int indexPas; // Increment/decrement per cicle de computació (= índex del array 'passos')
	
	Propulsor(int maxP, int idP, CyclicBarrier fiDeCicle) {
		this.maxP = new MutableInt(maxP);
		actP = new MutableInt(0);
		dstP = new MutableInt(0);
		prvP = new MutableInt(0);
		width = (int)Math.log10(maxP) + 1;
		this.idP = idP;
		this.fiDeCicle = fiDeCicle;
		indexPas = 0; // ±1 per defecte
	}
	
	@Override
	public void run() {
		prvP.value = actP.value; // En cualquier caso, se almacena el valor del ciclo anterior
		int diffP = dstP.value - actP.value;
		if(diffP > 0)
			actP.value = Math.min(actP.value+passos[indexPas], maxP.value);
		else if(diffP < 0)
			actP.value = Math.max(actP.value-passos[indexPas], 0);
		try { fiDeCicle.await(); } catch (InterruptedException | BrokenBarrierException e) {}
	}
	
	void reset() {
		actP.value = 0;
		dstP.value = 0;
		prvP.value = 0;
	}
	
}
