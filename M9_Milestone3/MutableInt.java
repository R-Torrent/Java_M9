package M9_Milestone3;

/*
 * En Java, por desgracia, las clases "wrapper" como Integer, Double, etc., son inmutables:
 * Al igual que con los String, la magnitud almacenada en estas clases NO SE PUEDE ALTERAR.
 * Cuando el valor cambia, la JVM crea un nuevo objeto. Y si dos objetos coinciden en valor,
 * tendrán asignado el mismo #Id (!!) Además, el lenguaje llama a los métodos «por valor» y
 * no «por referencia». Así, resulta complicadísmo (y absurdo) actualizar objetos compuestos
 * por muchísimos enteros variables, como los paneles del 'Milestone 3', sólo con los métodos
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
