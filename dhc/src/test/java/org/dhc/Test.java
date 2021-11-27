package org.dhc;



import java.awt.AWTException;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.io.IOException;

public class Test {

	public static void main(String[] args) throws AWTException, IOException {
		Robot robot = new Robot();
		int i = 0;
		while(true) {
			System.out.println(i);
			robot.delay(60000);
			robot.keyPress(KeyEvent.VK_F15);
			i++;
			if(i>60) {
				break;
			}
			
		}
	}
}

