package main.java.powder;

import java.util.Random;

import main.java.powder.particles.Particle;


public class Game extends Thread {
	
	static boolean paused = false;
	static Display.FPS gfps = new Display.FPS();
	static Random r = new Random();
	
	public static final double MIN_TEMP = -273.15;
	public static final double MAX_TEMP = 9725.85;
	
	static void update() {
		for (int w = 0; w < Display.width; w++) {
			for (int h = 0; h < Display.height; h++) {
				Particle p;
				if((p = Cells.getParticleAt(w, h))!=null) {
					try {
						p.update();
					} catch (NullPointerException e) {}
				}
			}
		}
		gfps.add();
	}

	public void startUpdateThread() {
		start();
	}

	public void run() {
		gfps.start();
		while (isAlive()) {
			if(Cells.cells[0][0] != null && !paused) update();
			try {
				Thread.sleep(25);
			} catch (InterruptedException e) {}
		}
	}


}
