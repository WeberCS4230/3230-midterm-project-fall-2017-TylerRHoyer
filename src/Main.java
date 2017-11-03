import java.io.IOException;

public class Main {

	public static void main(String[] args) throws InterruptedException {

		try {
			new Client();
		} catch (IOException e) {
			System.out.println("Failed to start the game client.\n");
			e.printStackTrace();
		}
		Thread.sleep(1000000);
		
	}

}
