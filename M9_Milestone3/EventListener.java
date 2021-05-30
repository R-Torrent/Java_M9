package M9_Milestone3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * Aplicación del patrón de diseño "Observer (Behavioral)": Interfaz para los
 * observadores y un gestor para que el sujeto puede administrar múltiples eventos.
 */
public interface EventListener {
	
	void update(String eventType, MutableInt observed);
	
}

class EventManager {
	
	Map<String, List<EventListener>> listeners = new HashMap<>();
	
	EventManager(String ...eventTypes) {
		for(String eventType : eventTypes)
			addEvent(eventType);
	}
	
	List<EventListener> addEvent(String eventType) {
		return listeners.put(eventType, new ArrayList<>());
	}
	
	List<EventListener> removeEvent(String eventType) {
		return listeners.remove(eventType);
	}
	
	boolean addListener(String eventType, EventListener listener) {
		return listeners.get(eventType).add(listener);
	}
	
	boolean removeListener(String eventType, EventListener listener) {
		return listeners.get(eventType).remove(listener);
	}
	
	void notifyListeners(String eventType, MutableInt observed) {
		listeners.get(eventType).forEach(listener -> listener.update(eventType, observed));
	}
	
}
