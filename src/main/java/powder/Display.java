package main.java.powder;

import main.java.powder.elements.Element;
import main.java.powder.elements.Elements;
import main.java.powder.particles.Particle;

import javax.swing.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

public class Display extends JPanel implements ActionListener, KeyListener, MouseListener, MouseMotionListener, MouseWheelListener {

	final static int width = 612;
	final static int height = 384;
	private static final long serialVersionUID = 1L;
	static int img_scale = 1;
	
	static int cell_w = 0;
	static int cell_h = 0;
	
	static boolean small = true;
	static Graphics2D w2d;
	static BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
	static Graphics2D b2d = img.createGraphics();
	static Font typeface = new Font("Monospaced", Font.PLAIN, 11);
	static Element left = Elements.dust; // Hacky as fuck.
	static Element right = Elements.none;
	static FPS dfps = new FPS();
	static int view = 0;
	static String viewName = "Default";
	
	public Timer timer = new Timer(5, this);
	public Point mouse = new Point(0,0);
	public Game game = new Game();
	public int size = 0;
	public int draw_size = 0;
	public Point mstart = new Point(0, 0), mstop = new Point(0, 0);
	
	public Display() {
		for (int w = 0; w < Display.width; w++)
			for (int h = 0; h < Display.height; h++) 
				Cells.cells[w][h] = new Cell(w, h);
		for (int w = 0; w < Display.width/4; w++)
			for (int h = 0; h < Display.height/4; h++) 
				Cells.cellsb[w][h] = new BigCell(w, h);
		game.startUpdateThread();
		timer.start();
		dfps.start();

		setFocusable(true);
		addKeyListener(this);
		addMouseListener(this);
		addMouseWheelListener(this);
		addMouseMotionListener(this);

		BufferedImage cursorImg = new BufferedImage(16, 16, BufferedImage.TYPE_4BYTE_ABGR);
		Cursor blankCursor = Toolkit.getDefaultToolkit().createCustomCursor(cursorImg, new Point(0, 0), "");
		setCursor(blankCursor);
		
		setKeyBindings();
	}

	static void makeSmall() {
		cell_w = 0;
		cell_h = 0;
		img_scale = 1;
		img = new BufferedImage(width * img_scale, height * img_scale, BufferedImage.TYPE_4BYTE_ABGR);
		b2d = img.createGraphics();
		small = true;
		Window.window.resize();
	}

	static void makeLarge() {
		cell_w = 1;
		cell_h = 1;
		img_scale = 2;
		img = new BufferedImage(width * img_scale, height * img_scale, BufferedImage.TYPE_4BYTE_ABGR);
		b2d = img.createGraphics();
		small = false;
		Window.window.resize();
	}
	
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		w2d = (Graphics2D) g;

		b2d.setColor(Color.BLACK);
		b2d.fillRect(0, 0, getWidth(), getHeight());
		size = 0;
		for(int w=0; w<width; w++)
			for(int h=0; h<height; h++)
				draw_cell(Cells.cells[w][h]);

		b2d.setColor(Color.LIGHT_GRAY);
		int sx = mstart.x * img_scale; int w = (mstop.x-mstart.x) * img_scale;
		int sy = mstart.y * img_scale; int h = (mstop.y-mstart.y) * img_scale;
		b2d.drawRect(sx, sy, w, h);
		//b2d.drawOval(sx, sy, w, h);
		int mx = sx + w / 2;
		int my = sy + h / 2;
		b2d.drawRect(mx, my, img_scale - 1, img_scale - 1);
		b2d.drawLine(mx, 0, mx, 4); 
		b2d.drawLine(mx, getHeight()-4,mx, getHeight());
		b2d.drawLine(0, my, 4, my);
		b2d.drawLine(getWidth() - 4, my, getWidth(), my);
		b2d.setColor(new Color(244, 244, 244, 32));
		b2d.fillRect(sx, sy, w, h);
		
		
		w2d.drawImage(img, null, 0, 0);
		w2d.setColor(Color.WHITE);
		w2d.setXORMode(Color.BLACK);
		w2d.setFont(typeface);
		int line = 1;
		int spacing = w2d.getFontMetrics().getHeight();
		w2d.drawString("FramesPS    "+dfps.fps(), 5, spacing*line++);
		w2d.drawString("UpdatesPS   "+Game.gfps.fps(), 5, spacing*line++);
		w2d.drawString("Selected    "+left.description, 5, spacing*line++);
		w2d.drawString("'Space'     "+(Game.paused ? "Paused" : "Playing"), 5, spacing*line++);
		w2d.drawString("'F'         Frame", 5, spacing*line++);
		w2d.drawString("Display     "+viewName, 5, spacing*line++);
		w2d.drawString("Parts       "+size, 5, spacing*line++);

		w2d.drawString("X:"+mouse.x+" Y:"+mouse.y, 5, getHeight()-25);
		Particle p;
		String info = "Empty";
		if((p = Cells.getParticleAt(mouse.x, mouse.y))!=null) {
			if(p.el!=null) {
				info = p.el.shortName;
				if(!(p.ctype==0) && Elements.exists(p.ctype)) info += "("+Elements.get(p.ctype)+")";
				info += ", Temp:"+p.temp();
				info += ", Life:"+p.life;
			} else p.setRemove(true);
		}
		w2d.drawString(info, 5, getHeight()-10);
		
		dfps.add();
	}
	
	public void draw_cell(Cell c) {
		if (c.part == null) return;
		for (int pnum = 0; pnum < 9; pnum++) {
			if (c.part == null) continue; // Required for plutonium, because it does something special.
			// For some reason there's a race condition in here.
			if (c.part[pnum] != null && c.part[pnum].display()) {
				/*if (c.part[pnum].remove()) {
					// bad for performance
					c.part = null; // Why?
				} else {*/
				size++;
				try {
					b2d.setColor(c.part[pnum].getColor());
					if (view == 1) b2d.setColor(c.part[pnum].getTempColor());
					b2d.drawRect(c.screen_x(), c.screen_y(), cell_w, cell_h);
				} catch (NullPointerException e) {}
				//}
			}
		}
	}
	
	public void place(Element e) {
		for (int x = mstart.x; x <= mstop.x; x++) {
			for (int y = mstart.y; y <= mstop.y; y++) {
				Particle p = Cells.getParticleAt(x, y);
				if(p==null || e == Elements.none)
					Cells.setParticleAt(x, y, new Particle(e, x, y), e == Elements.none);
				else if(p.el.conducts && e==Elements.sprk) {
					p.morph(Elements.sprk, Particle.MORPH_KEEP_TEMP, true);
				} else if(p.el == Elements.clne) {
					p.ctype = e.id;
				}
			}
		}
	}
	
	public Point mouseToCell(Point p) {
		return new Point(p.x / img_scale, p.y / img_scale);
	}
	
	public Point mouseToBigCell(Point p) {
		return new Point(p.x / img_scale, p.y / img_scale);
	}

	public void updateMouse(Point p) {
		mouse = p;
		mstart = new Point(mouse.x-draw_size/2,mouse.y-draw_size/2);
		mstop = new Point(mstart.x+draw_size, mstart.y+draw_size);
	}

	public void mouseDragged(MouseEvent e) {
		Window.updateMouseInFrame(e.getPoint(), this);
		updateMouse(mouseToCell(e.getPoint()));
		if(SwingUtilities.isLeftMouseButton(e))
			place(left);
		if(SwingUtilities.isRightMouseButton(e))
			place(right);
	}

	public void mouseMoved(MouseEvent e) {
		Window.updateMouseInFrame(e.getPoint(), this);
		updateMouse(mouseToCell(e.getPoint()));
	}

	public void mouseClicked(MouseEvent e) {

	}

	public void mouseEntered(MouseEvent e) {

	}

	public void mouseExited(MouseEvent e) {

	}

	public void mousePressed(MouseEvent e) {
		if(SwingUtilities.isLeftMouseButton(e))
			place(left);
		if(SwingUtilities.isRightMouseButton(e))
			place(right);
		if(SwingUtilities.isMiddleMouseButton(e)) {
			Particle m = Cells.getParticleAt(mouse.x, mouse.y);
			if(m!=null) left = m.el;
		}
	}

	public void mouseReleased(MouseEvent e) {

	}

	public void keyPressed(KeyEvent e) {

	}

	public void keyReleased(KeyEvent e) {

	}

	public void keyTyped(KeyEvent e) {
		// Broken with focus issues.
	}
	
	public InputMap im = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
	public ActionMap am = getActionMap();
	
	@SuppressWarnings("serial")
	public void setKeyBindings() {
		addKeyBinding(' ', "pause", new AbstractAction(){
			public void actionPerformed(ActionEvent e) {
				Display.toggle_pause();
			}
		});
		addKeyBinding('s', "resize", new AbstractAction(){
			public void actionPerformed(ActionEvent e) {
				Display.toggle_size();
			}
		});
		addKeyBinding('f', "frame", new AbstractAction(){
			public void actionPerformed(ActionEvent e) {
				Game.paused = true;
				Game.update();
			}
		});
		addKeyBinding('[', "mouse_small", new AbstractAction(){
			public void actionPerformed(ActionEvent e) {
				draw_size-=2; updateMouse(mouse);
				if(draw_size<0) draw_size = 0;
			}
		});
		addKeyBinding(']', "mouse_big", new AbstractAction(){
			public void actionPerformed(ActionEvent e) {
				draw_size+=2; updateMouse(mouse);
				if(draw_size<0) draw_size = 0;
			}
		});
		addKeyBinding('1', "view1", new AbstractAction(){
			public void actionPerformed(ActionEvent e) {
				setView(0);
			}
		});
		addKeyBinding('2', "view2", new AbstractAction(){
			public void actionPerformed(ActionEvent e) {
				setView(1);
			}
		});
	}
	
	public void addKeyBinding(char c, String name, Action action) {
		im.put(KeyStroke.getKeyStroke(c), name);
		am.put(name, action);
	}
	
	static void setView(int i) {
		if(i==0) {
			view = 0;
			viewName = "Default";
		}
		if(i==1) {
			view = 1;
			viewName = "Temperature";
		}
	}
	
	static void toggle_size() {
		if(small) makeLarge(); else makeSmall();
	}
	
	static void toggle_pause() {
		 Game.paused = !Game.paused;
		 Window.window.menub.repaint();
	}
	
	public void mouseWheelMoved(MouseWheelEvent e) {
		draw_size-=e.getWheelRotation();
		if(draw_size<0) draw_size = 0;
		mstart = new Point(mouse.x-draw_size/2,mouse.y-draw_size/2);
		mstop = new Point(mstart.x+draw_size, mstart.y+draw_size);
	}
	
	public void actionPerformed(ActionEvent e) {
		repaint();
	}

	static class FPS extends Thread {
		public double seconds = 0;
		public long total = 0;
		public double avg = 0;
		
		public long count = 0;
		public long fps = 0;
		public long last_fps = System.currentTimeMillis();
		
		public void add() {
			count++;
		}
		
		public void run() {
			while(isAlive()) {
				if(System.currentTimeMillis()-last_fps > 1000) {
					seconds+=(System.currentTimeMillis()-last_fps) / 1000.0;
					
					fps = count;
					count = 0;
					last_fps = System.currentTimeMillis();
					
					total+=fps;
					avg = total / seconds;
					if(seconds > 60) resetAverage();
				}
				try {
					Thread.sleep(25);
				} catch (InterruptedException ignored) {
				}
			}
		}
		
		public long fps() {
			return fps;
		}
		
		public void resetAverage() {
			seconds = 0;
			total = 0;
			avg = 0;
		}
		
		public double average() {
			return Math.round(avg * 100.0) / 100.0;
		}
	}
}
