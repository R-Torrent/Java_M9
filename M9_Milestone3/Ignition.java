package M9_Milestone3;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Toolkit;

import java.awt.event.AWTEventListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import java.time.LocalTime;

import java.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.Timer;
import java.util.TimerTask;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/*
 *  Patrón abstracto para las misiones implementadas. Incluye la gestión multihilo de los propulsores.
 *  También incluye una aplicación del patrón de diseño "Observer (Behavioral)" para anunciar que el
 *  cohete ha alcanzado su potencia objetivo (Milestone2, heredado en Milestone3).
 */
public abstract class Ignition {
	
	Coet coet;
	
	static Toolkit tk;// Eines del sistema
	
	ExecutorService executor; // Assigna els 'threads' als propulsors durant el cicle de càlcul
	
	Timer coetTimer; // Temporitzador, "rellotge" del programa
		
	Ignition(Coet coet) {
		this.coet = coet;
		
		tk = Toolkit.getDefaultToolkit();
		
		// Concurrency: Threads preloaded into an 'ExecutorService' instance
		executor = Executors.newFixedThreadPool(coet.planta.length); // Un fil per a cada propulsor
	}
	
	// Método principal del programa. Ejecuta el control de potencia sobre el cohete en vuelo
	void maniobra(boolean firstBurn) {
		if(firstBurn) inici();
		
		encesa();
				
		if(firstBurn) seguiment();
		
		// Nuevo 'Thread' temporal repetitivo para sicronizar las tareas preasignadas a cada propulsor
		coetTimer = new Timer();
		coetTimer.scheduleAtFixedRate(new coetTimerTask(), 2000L, 2000L); // Període de dos segons
		
		// El 'Thread' principal (llamado 'main') muere aquí. El programa corre con los hilos de los
		// propulsores, el 'Timer' y el hilo 'AWT-EventQueue' que gestiona todos los eventos.
	}
	
	abstract void inici(); // Preburn
	
	abstract void encesa(); // Ignition
	
	abstract void seguiment(); // Telemetry (one-time constitution of event listeners)
	
	abstract void refresh(boolean timestamp); // Print motor status; with or without a 'timestamp'
	
	void distribute() { // Reparto de la potencia requerida entre los propulsores (dstPTotal -> p[i].dstP)
		for(Propulsor p : coet.planta)
			p.dstP.value = 0;
		for(int i = 0, j= 0; j < coet.dstPTotal.value; i++) {
			Propulsor p = coet.planta[i % coet.planta.length];
			if(p.dstP.value < p.maxP.value) {
				p.dstP.incMutableInt();
				j++;
			}
		}
	}
	
	void end(String s) {
		coetTimer.cancel();
		executor.shutdownNow();
		tk.beep();
		System.out.println(s);
		System.exit(0);
	}
	
	// Tasca executada per 'coetTimer' a cada cicle
	class coetTimerTask extends TimerTask {
		
		@Override
		public void run() {
			for(Propulsor p : coet.planta)
				executor.execute(p); // Un fil per a cada propulsor
			try { coet.fiDeCicle.await(); // El fil temporal s'espera a que tots els fils propulsors acabin...
			} catch (InterruptedException | BrokenBarrierException e) {}
			coet.fiDeCicle.reset();
			
			refresh(true); // ...abans de imprimir els resultats
		}
		
	}
	
}

// Resolución del enunciado Milestone 1. Potencia requerida libre
class Milestone1 extends Ignition {
	
	Milestone1(Coet c) {
		super(c);
	}
	
	@Override
	void inici() {
		System.out.println("Opcions:");
		System.out.println("Apujar potència  [w] +1  [W] +10  [Ctrl+w] +100  [Ctrl+W] +1000");
		System.out.println("Abaixar potència [s] -1  [S] -10  [Ctrl+s] -100  [Ctrl+S] -1000");
		System.out.println("Autodestrucció   [q] [Q] [Ctrl+q] [Ctrl+Q]");
		
		new ReminderFrame(coet.codi);
	}
	
	@Override
	void encesa() {
		String[] burn = new String[11];
		burn[0] = coet.codi + "...";
		for(int i = 10; i > 0; i--) burn[11-i] = String.format("%2d...", i);
		
		for(int i = 0; i < burn.length; i++) {
			System.out.println(burn[i]);
			try { Thread.sleep(1000L); } catch (InterruptedException e) {}
		}
		System.out.println("IGNICIÓ!");
		tk.beep();
	}
	
	@Override
	void seguiment() {
        // El control de potencia funciona monitorizando los eventos de teclado
		tk.addAWTEventListener(new AWTEventListener() {
			
			boolean shft = false;
			boolean ctrl = false;
			
			@Override
			public void eventDispatched(AWTEvent event) {
				if(event.getID() == KeyEvent.KEY_PRESSED) {
					switch(((KeyEvent)event).getKeyCode()) {
					case KeyEvent.VK_SHIFT:
						shft = true;
						return;
					case KeyEvent.VK_CONTROL:
						ctrl = true;
						return;
					case KeyEvent.VK_W:
						if(coet.dstPTotal.value == coet.maxPTotal.value) return;
						coet.dstPTotal.value = Math.min(coet.dstPTotal.value+amount(), coet.maxPTotal.value);
						break;
					case KeyEvent.VK_S:
						if(coet.dstPTotal.value == 0) return;
						coet.dstPTotal.value = Math.max(coet.dstPTotal.value-amount(), 0);
						break;
					case KeyEvent.VK_Q:
						end("\nBUUUUUUUUUUM!");
					default:
						return;
					}
					
					distribute();
					
					refresh(false);
				}
				else if(event.getID() == KeyEvent.KEY_RELEASED) {
					switch(((KeyEvent)event).getKeyCode()) {
					case KeyEvent.VK_SHIFT:
						shft = false;
						break;
					case KeyEvent.VK_CONTROL:
						ctrl = false;
					default:
					}
				}
			}
			
			int amount() {
				if(!shft && !ctrl) return 1;
				else if(shft) return !ctrl ? 10 : 1000;
				else return 100;
			}
			
		}, AWTEvent.KEY_EVENT_MASK);
	}
	
/*
 * De fábrica, la consola de Eclipse no acepta colores ni caracteres Unicode ni códigos de escape ANSI...
 * Tampoco se puede controlar el cusor y el juego de caracteres se reduce al Windows-1252, codificación
 * ¡de un solo byte! En una palabra, una porquería.
 */
	@Override
	void refresh(boolean timestamp) { // Impresión por consola del estado del cohete
		if(timestamp) // Corre el registro de potencia total 'actual' a 'previa' antes del siguiente paso temporal
			coet.prvPTotal.value = coet.actPTotal.value;
		coet.actPTotal.value = 0;
		StringJoiner textPs = new StringJoiner(" ");
		char canvi;
		for(Propulsor p : coet.planta) {
			coet.actPTotal.value += p.actP.value; // Suma de potencia total
			
			if(!timestamp || p.actP.value == p.prvP.value)
				canvi = ' ';
			else if(p.actP.value > p.prvP.value)
				canvi = '!';
			else
				canvi = '¡';
			textPs.add(String.format("%c%"+p.width+"s(%"+p.width+"s)/%s", canvi, p.actP, p.dstP, p.maxP));
		}
		String stamp = timestamp ? " <-- " + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) : "";
		if(timestamp) System.out.println();
		System.out.println(String.format("%s || pot. total: %"+coet.widthTotal+"s/%s || pot. desitjada: %"+coet.widthTotal+"s%s",
				textPs.toString(), coet.actPTotal, coet.maxPTotal, coet.dstPTotal, stamp));
	}
	
/*
 * Este 'frame' minimalista existe porque la consola de Eclipse es un desastre. Se pretende
 * controlar la potencia del cohete con las teclas 'W' y 'S' sin pulsar la tecla <ENTER> en
 * cada modificación del impulso. La única mánera es construyendo un contenedor, aunque sea
 * un adorno, para que funcione el AWT Event Queue.
 */
	class ReminderFrame extends JFrame {
		
		static final long serialVersionUID = 1L;
		
		JPanel panel;
		
		ReminderFrame(String s) {
			super(s);
			
			panel = new JPanel();
			panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
			panel.add(new JLabel("Opcions:  Apujar potència   [w] +1  [W] +10  [Ctrl+w] +100  [Ctrl+W] +1000"));
			panel.add(new JLabel("          Abaixar potència  [s] -1  [S] -10  [Ctrl+s] -100  [Ctrl+W] -1000"));
			panel.add(new JLabel("          Autodestrucció    [q] [Q] [Ctrl+q] [Ctrl+Q]"));
			
			Font font = new Font("Monospaced", Font.BOLD, 15);
			for(Component c : panel.getComponents())
				c.setFont(font);
			
			add(panel);
			
			pack();
	        setLocationRelativeTo(null);
			setVisible(true);
			
			setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		}
		
	}
	
}

// Resolución del enunciado Milestone 2. Potencia prefijada por la aceleración demandada
class Milestone2 extends Milestone1 {
	
	EventManager events; // Gestor d'esdeveniments relacionats amb les potències del coet
	
	Milestone2(Coet c) {
		super(c);
		
		events = new EventManager("ObjPot"); // Monitora l'arribada a la potència objectiu
	}
	
	@Override
	void inici() {
		int vel1 = 0, vel2 = 0;
		try {
			System.out.println("( Velocitats < 0 són permeses; representen el sentit invers a l'establert. )");
			System.out.println("Velocitat_inicial:");
			System.out.println("? ");
			vel1 = Main.sc.nextInt();
			System.out.println("( |Velocitat_final| < |Velocitat_inicial| és permès; el coet es pot frenar orientant les toberes endavant. )");
			System.out.println("Velocitat_final:");
			System.out.println("? ");
			vel2 = Main.sc.nextInt();
		} catch (RuntimeException e) {
			System.out.println("Error: Velocitat incorrecte!");
			System.exit(128); // Dins UNIX, 128 és un codi de sortida reservat: "Invalid argument to exit"
		}
		
		coet.dstPTotal.value = PotReqPerAccel(vel1, vel2);
		if(coet.maxPTotal.value < coet.dstPTotal.value) {
			System.out.println("Error: Potència insuficient!");
			System.out.println(coet.codi + ": Potència màxima    " + coet.maxPTotal);
			System.out.println(String.format("%"+coet.codi.length()+"s", "") + "  Potència requerida " + coet.dstPTotal);
			
			System.exit(126); // Dins UNIX, 126 és un codi de sortida reservat: "Command invoked cannot execute"
		}
		
		distribute();
	}
	
	@Override
	void seguiment() {
		events.addListener("ObjPot", new EventListener() { // Subscripción al servicio de alertas "ObjPot"
			
			@Override
			public void update(String eventType, MutableInt observed) {
				end("\nÈxit: Acceleració assolida! Potència " + observed);
			}
			
		});
	}
	
	// Impresión por consola del estado del cohete
	@Override
	void refresh(boolean timestamp) {
		super.refresh(timestamp);
		
		if(coet.actPTotal.value == coet.dstPTotal.value) // Notificación de que el cohete alcanzó su potencia objetivo
			events.notifyListeners("ObjPot", coet.actPTotal);
	}
	
	// Potència total requerida [ Veure la NOTA 3 dins el fitxer Main.java ]
	int PotReqPerAccel(int vel1, int vel2) {
		return (int)Math.ceil(Math.pow((vel2-vel1)/100.0, 2.0));
	}
	
}

// Resolución del enunciado Milestone 3. Interrupciones e interfaz gráfica
class Milestone3 extends Milestone2 implements Runnable {
	
	static int maniobras = 1; // Número de maniobra
	
	static PrintStream old = System.out; // "Standard" output stream
	
	MutableInt actVel, dstVel; // Velocitats actual i desitjada del coet
	
	EngineControl control; // Panel de control (interfaz gráfica)
	
	Boolean pausaManiobra; // Maniobra pausada ( <- botón "Pausa" )
	
	Milestone3(Coet c) {
		this(c, new MutableInt(0));
	}
	
	Milestone3(Coet c, MutableInt velInicial) {
		super(c);
		
		actVel = velInicial;
		dstVel = new MutableInt(velInicial);
		pausaManiobra = false;
	}
	
	@Override
	public void run() { // Cada maniobra posterior a la 1ª arranca desde un nuevo hilo con este método
		maniobra(false);
	}
	
	@Override
	void inici() {
		control = new EngineControl(coet.codi + "  --  Maniobra " + Milestone3.maniobras);
		
		System.setOut(new PrintStream(new InfoStream(control.info))); // Redirecciona la salida por consola al panel
		
		control.dstVelJTF.getDocument().addDocumentListener(new DocumentListener() {
			
			@Override
			public void insertUpdate(DocumentEvent e) {
				documentChanged(e);
			}
			
			@Override
			public void removeUpdate(DocumentEvent e) {
				documentChanged(e);
			}
			
			@Override
			public void changedUpdate(DocumentEvent e) {
				documentChanged(e);
			}
			
			// Lectura de la velocidad final demandada
			void documentChanged(DocumentEvent ev) {
				try {
					dstVel.value = Integer.parseInt(control.dstVelJTF.getText());
					int PropostaP = PotReqPerAccel(actVel.value, dstVel.value);
					// Potencia demandada excesiva ?
					control.dstVelJTF.setForeground(PropostaP > coet.maxPTotal.value ? Color.RED : Color.BLACK);
					coet.dstPTotal.value = Math.min(PropostaP, coet.maxPTotal.value);
				} catch (NumberFormatException ex) {
					control.dstVelJTF.setForeground(Color.RED);
					coet.dstPTotal.value = 0;
				} finally {
					distribute(); // Nuevas órdenes a los propulsores
					refresh(false); // Presentación de la nueva condición
				}
			}
			
		});
		
		// Control individual del paso de los propulsores
		for(Component c : control.grid.getComponents()) {
			EngineControl.PropControl pControl = (EngineControl.PropControl)c;  
			pControl.comboPas.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					pControl.p.indexPas = pControl.comboPas.getSelectedIndex();
				}
				
			});
		}
		
		// Permite al usuario arrancar la simulación a su conveniencia
		control.ignicio.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				control.ignicio.setEnabled(false);
				control.dstVelJTF.requestFocusInWindow();
				synchronized(Milestone3.this) { Milestone3.this.notify(); }
			}
			
		});
		
	}
	
	@Override
	synchronized void encesa() {
		try {  wait();  // El programa espera aquí a que el usuario pulse "Ignició!"
		} catch (InterruptedException e) {}
		
		super.encesa();
		
		control.pausa.setEnabled(true);
		control.sortida.setEnabled(true);
	}
	
	@Override
	void seguiment() {
		events.addListener("ObjPot", new EventListener() { // Subscripción al servicio de alertas "ObjPot"
			
			@Override
			public void update(String eventType, MutableInt observed) {
				coetTimer.cancel(); // Congela la simulación...
				control.pausa.setEnabled(false);
				control.sortida.setEnabled(false);
				new RocketBurn(); // ...y presenta el panel de resultados
			}
			
		});
		
		control.pausa.addActionListener(new ActionListener() { // Esdeveniment "Pausa"
			
			@Override
			public void actionPerformed(ActionEvent e) {
				if(!pausaManiobra) {
					coetTimer.cancel();
					System.out.println("<Pausa>");
				}
				else {
					coetTimer = new Timer();
					coetTimer.scheduleAtFixedRate(new coetTimerTask(), 2000L, 2000L); // Període de dos segons
					System.out.println();
				}
				
				pausaManiobra = !pausaManiobra;
			}
			
		});
		
		control.sortida.addActionListener(new ActionListener() { // Esdeveniment "Sortida"
			
			@Override
			public void actionPerformed(ActionEvent e) {
				end("\nAdéu!");
			}
			
		});
	}
	
	@Override
	void refresh(boolean timestamp) {
		if(timestamp) // Corre el registro de potencia total 'actual' a 'previa' antes del siguiente paso temporal
			coet.prvPTotal.value = coet.actPTotal.value;
		coet.actPTotal.value = 0;
		for(Propulsor p : coet.planta)
			coet.actPTotal.value += p.actP.value; // Suma de potencia total
		
		control.indicadors.forEach(engineIndicator -> engineIndicator.updateValue()); // Actualización de los indicadores
		
		if(timestamp && coet.actPTotal.value == coet.dstPTotal.value)
			// Notificación de que el cohete alcanzó su potencia objetivo
			events.notifyListeners("ObjPot", coet.actPTotal);
	}
	
	@Override
	void end(String s) {
		System.setOut(old); // Recupera la salida por consola
		super.end(s);
	}
	
	// Imprime un 'stream' de bytes en una JLabel
	class InfoStream extends OutputStream {
		
		StringBuffer buffer;
		JLabel label;
		
		InfoStream(JLabel info) {
			buffer = new StringBuffer(128);
			label = info;
		}
		
		@Override
		public void write(int b) throws IOException {
			if(b == '\n') {
				label.setText(buffer.toString()); // Coloca 'buffer' en el panel
				buffer.delete(0, buffer.length()); // Vacía el buffer
			}
			else
				buffer.append((char)(b & 0xFF)); // Añade el carácter nuevo
/*
 * NOTA: La «extraña» máscara 0xFF está allí porque «algo» añade bits a los caracteres con acento
 * dentro de las constantes String en las llamadas System.out.println(). Unicode (internamente,
 * Java usa UTF-16) permite codificar letras con acento de dos maneras: directamente con su código
 * o combinando la letra básica con el acento agudo (\0301). Pero Java (o quizás Eclipse) no hizo
 * ninguna de las dos cosas:
 *   ó \u00F3 (alt. \u006F \u0301) misteriosamente transformado en \uFFFF \uFFF3
 *   Ó \u00D3 (alt. \u004F \u0301) misteriosamente transformado en \uFFFF \uFFD3
 * El apaño funciona porque (afortunadamente) todos los caracteres de este programa, y concretamente
 * los que se redirigen al JLabel, se pueden codificar con un solo byte.
 */
		}
		
	}
	
	// Ventana interactiva para controlar el funcionamiento del cohete
	class EngineControl extends JFrame {
		
		static final long serialVersionUID = 1L;
		
		List<EngineIndicator> indicadors; // Tots els indicadors variables al tauler de control
		JTextField dstVelJTF;
		JLabel info; // 'OutputStream' amb els missatges de la consola
		JButton ignicio, pausa, sortida;
		Font font = new Font("Monospaced", Font.BOLD, 15);
		JPanel grid;
		
		// Panel principal de mando
		EngineControl(String s) {
			super(s);
			
			indicadors = new ArrayList<>();
			
			setLayout(new BorderLayout(0, 5));
			setPreferredSize(new Dimension(400, 250));
			
			Box boxSuperior = Box.createHorizontalBox();
			
			Box boxPotencies = Box.createVerticalBox();
			boxPotencies.add(Box.createVerticalStrut(9));
			JLabel labelP = new JLabel("POTÈNCIES");
			labelP.setFont(font);
			boxPotencies.add(labelP);
			boxPotencies.add(new EngineIndicator("Actual:    ", coet.actPTotal, coet.prvPTotal, font, coet.widthTotal));
			boxPotencies.add(new EngineIndicator("Desitjada: ", coet.dstPTotal, font, coet.widthTotal));
			boxPotencies.add(new EngineIndicator("Màxima:    ", coet.maxPTotal, font, coet.widthTotal));
			boxSuperior.add(boxPotencies);
			
			boxSuperior.add(Box.createHorizontalStrut(20));
			
			Box boxVelocitats = Box.createVerticalBox();
			boxVelocitats.add(Box.createVerticalStrut(9));
			JLabel labelV = new JLabel("VELOCITATS");
			labelV.setFont(font);
			boxVelocitats.add(labelV);
			boxVelocitats.add(new EngineIndicator("Actual: ",actVel, font, 0));
			
			Box boxVelDesitjada = Box.createHorizontalBox();
			boxVelDesitjada.add(new JLabel("Desitjada: "));
			dstVelJTF = new JTextField(dstVel.toString());
			dstVelJTF.setColumns(5);
			dstVelJTF.setMaximumSize(dstVelJTF.getPreferredSize());
			boxVelDesitjada.add(dstVelJTF);
			boxVelDesitjada.setAlignmentX(Component.LEFT_ALIGNMENT);
			for(Component c : boxVelDesitjada.getComponents())
				c.setFont(font);
			boxVelocitats.add(boxVelDesitjada);
			
			info = new JLabel();
			info.setForeground(Color.BLUE);
			info.setBorder(BorderFactory.createEtchedBorder());
			info.setPreferredSize(boxVelDesitjada.getPreferredSize());
			info.setMaximumSize(info.getPreferredSize());
			boxVelocitats.add(info);
			
			boxSuperior.add(boxVelocitats);
			
			boxSuperior.add(Box.createHorizontalStrut(20));
			
			Box boxBotons = Box.createVerticalBox();
			boxBotons.add(Box.createVerticalStrut(5));
			ignicio = new JButton("Ignició!");
			boxBotons.add(ignicio);
			pausa = new JButton(" Pausa ");
			pausa.setEnabled(false);
			boxBotons.add(Box.createVerticalStrut(10));
			boxBotons.add(pausa);
			sortida = new JButton("Sortida");
			sortida.setEnabled(false);
			boxBotons.add(Box.createVerticalStrut(10));
			boxBotons.add(sortida);
			
			boxBotons.setMaximumSize(boxBotons.getPreferredSize());
			for(Component c : boxBotons.getComponents())
				((JComponent)c).setAlignmentX(Component.CENTER_ALIGNMENT);
			boxSuperior.add(boxBotons);
			
			boxSuperior.add(Box.createHorizontalGlue());
			
			add(boxSuperior, BorderLayout.NORTH);
			for(Component c : boxSuperior.getComponents())
				((JComponent)c).setAlignmentY(Component.TOP_ALIGNMENT);
			
			// Panel individual para cada propulsor
			GridLayout gl = new GridLayout(0, coet.planta.length);
			gl.setHgap(5);
			grid = new JPanel(gl);
			for(Propulsor p : coet.planta)
				grid.add(new PropControl(p));
			
			JScrollPane scroll = new JScrollPane(grid);
			add(scroll, BorderLayout.CENTER);
			
			pack();
	        setLocationRelativeTo(null);
			setVisible(true);
			
			setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		}
		
		// Paneles secundarios para cada propulsor
		class PropControl extends Box {
			
			private static final long serialVersionUID = 1L;
			
			Propulsor p;
			
			JComboBox<Integer> comboPas;
			Font font = new Font("Monospaced", Font.BOLD, 12);
			
			PropControl(Propulsor p) {
				super(BoxLayout.Y_AXIS);
				this.p = p;
				
				JLabel labelP = new JLabel("Prop. " + (p.idP+1));
				labelP.setFont(font);
				add(labelP);
				add(new EngineIndicator("Actual:    ", p.actP, p.prvP, font, p.width));
				add(new EngineIndicator("Desitjada: ", p.dstP, font, p.width));
				add(new EngineIndicator("Màxima:    ", p.maxP, font, p.width));
				
				Box boxPas = Box.createHorizontalBox();
				boxPas.add(new JLabel("Pas: " ));
				comboPas = new JComboBox<Integer>(Propulsor.passos);
				comboPas.setSelectedIndex(p.indexPas);
				comboPas.setMaximumSize(new Dimension(50, 16));
				boxPas.add(comboPas);
				
				add(boxPas);
				boxPas.setAlignmentX(Component.LEFT_ALIGNMENT);
				for(Component c : boxPas.getComponents())
					c.setFont(font);
			}
			
		}
		
/*
 *  Indicador de magnitud variable. Precisamente esta clase fue la que forzó la inclusión
 *  del apaño 'MutableInt' en el programa.
 */
		class EngineIndicator extends Box {
			
			private static final long serialVersionUID = 1L;
			
			String label;
			MutableInt mInt, prvMInt;
			int width;
			
			JLabel jLabelLb, jLabelMI;
			
			EngineIndicator(String label, MutableInt mInt, Font font, int width) {
				this(label, mInt, null, font, width);
			}
			
			EngineIndicator(String label, MutableInt mInt, MutableInt prvMInt, Font font, int width) {
				super(BoxLayout.X_AXIS);
				this.label = label;
				this.mInt = mInt;
				this.prvMInt = prvMInt;
				this.width = width; // 0 significa que no hay anchura mínima (caso de la velocidad actual)
				
				jLabelLb = new JLabel(label);
				jLabelMI = new JLabel(width == 0 ? mInt.toString() : String.format("%"+width+"s", mInt));
				
				indicadors.add(this);
				
				add(jLabelLb);
				add(jLabelMI);
				jLabelLb.setFont(font);
				jLabelMI.setFont(font);
				setAlignmentX(Component.LEFT_ALIGNMENT);
			}
			
			void updateValue() { // Actualización del campo, sin recurrir a métodos '.get()' particulares
				jLabelMI.setText(width == 0 ? mInt.toString() : String.format("%"+width+"s", mInt));
				if(prvMInt != null) // La evolución de la potencia instantánea viene codificada con colores
					if(mInt.value > prvMInt.value)
						jLabelMI.setForeground(Color.GREEN);
					else if(mInt.value < prvMInt.value) jLabelMI.setForeground(Color.RED);
					else jLabelMI.setForeground(Color.BLACK);
			}
			
		}
		
	}
	
	// Escueta presentación con los resultados de la maniobra de encendido
	class RocketBurn extends JFrame {
		
		static final long serialVersionUID = 1L;
		
		boolean contabiliza = true;
		JPanel panelBotones;
		JTextArea areaMensaje;
		JButton si, no;
		
		RocketBurn() {
			super(coet.codi);
			
			String mensaje;
			if(dstVel.value == actVel.value) {
				mensaje = "Maniobra abortada. Manteniendo la velocidad ";
				contabiliza = false;
				System.out.println("Maniobra abortada");
			}
			else {
				mensaje = "Obtenida la potencia objetivo " + coet.actPTotal.toString()
				+ ", el cohete " + (dstVel.value > actVel.value ? "aceleró" : "frenó")
				+ " uniformemente hasta alcanzar la velocidad ";
				System.out.println("Acceleració assolida");
			}
			mensaje += dstVel.toString() + ". ¿Desea realizar otra maniobra?";
			areaMensaje = new JTextArea(mensaje);
			areaMensaje.setEditable(false);
			areaMensaje.setFont(control.font);					
			areaMensaje.setMargin(new Insets(10, 15, 10, 15));
			areaMensaje.setBackground(UIManager.getColor("Panel.background"));
			areaMensaje.setPreferredSize(new Dimension(345, 115));
			areaMensaje.setLineWrap(true);
			areaMensaje.setWrapStyleWord(true);
			
			add(areaMensaje, BorderLayout.CENTER);
			
			panelBotones = new JPanel();
			si = new JButton("Sí");
			no = new JButton("No");
			panelBotones.add(si);
			panelBotones.add(Box.createHorizontalStrut(20));
			panelBotones.add(no);
			
			add(panelBotones, BorderLayout.SOUTH);
			
			pack();
	        setLocationRelativeTo(null);
			setVisible(true);
			
			setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			
			si.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					dispose(); // La ventana se cierra a sí misma
					coet.reset(); // Todas las variables de motor a cero (= motor apagado)
					actVel.value = dstVel.value;
					refresh(false);
					control.ignicio.setEnabled(true);
					if(contabiliza) control.setTitle(coet.codi + "  --  Maniobra " + ++Milestone3.maniobras);
					System.out.println();
					
					// No puede ser que el hilo 'AWT-EventQueue' también gestione la nueva maniobra. Se
					// bloquearía irremediablemente esperando la señal del botón "Ignició!".
					new Thread(Milestone3.this).start(); // Equivale a "maniobra(false);" con un nuevo hilo	
				}
				
			});
			
			no.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					if(!contabiliza) Milestone3.maniobras--;
					end("\nMissió complerta! Velocitat " + dstVel.toString() + " aconseguida en "
						+ Milestone3.maniobras + " maniobr" + (Milestone3.maniobras == 1 ? "a." : "es."));
				}
				
			});
			
		}
		
	}
	
}
