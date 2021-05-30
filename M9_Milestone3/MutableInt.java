package M9_Milestone3;

/*
 * En Java, por desgracia, las clases "wrapper" como Integer, Double, etc., son inmutables:
 * Al igual que con los String, la magnitud almacenada en estas clases NO SE PUEDE ALTERAR.
 * Cuando el valor cambia, la JVM crea un nuevo objeto. Y si dos objetos coinciden en valor,
 * tendr�n asignado el mismo #Id (!!) Adem�s, el lenguaje llama a los m�todos �por valor� y
 * no �por referencia�. As�, resulta complicad�smo (y absurdo) actualizar objetos compuestos
 * por much�simos enteros variables, como los paneles del 'Milestone 3', s�lo con los m�todos
 * elementales '.get()' y '.set()'.
 */
public class MutableInt {
	
	int value;
	
    MutableInt(int i) {
        this.value = i;
    }
    
    MutableInt(MutableInt mi) {
        this.value = mi.value;
    }
    
    MutableInt(String s) throws NumberFormatException {
    	this.value = Integer.parseInt(s);
    }
    
    void incMutableInt() {
    	value++;
    }
    
    void decMutableInt() {
    	value--;
    }
    
    @Override
    public String toString() {
        return String.valueOf(value);
    }
    
}
