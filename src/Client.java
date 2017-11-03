import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Vector;

import blackjack.game.Card;
import blackjack.message.*;
import blackjack.message.Message.MessageType;

public class Client extends Thread {
	
	private enum GameState {
		UNAUTHENTICATED, REQUESTED_LOGIN, ENDED, REQUESTED_START, STARTED, REQUESTED_JOIN, JOINED
	}
	
	private String username = "defaultUsername";
	private GameWindow window;
	private Socket socket;
	private ObjectOutputStream out;
	private ObjectInputStream in;
	private GameState state = GameState.UNAUTHENTICATED;
	
	private Vector<Card> cards;
	
	public Client() throws UnknownHostException, IOException {
		cards = new Vector<>();
		socket = new Socket("ec2-54-172-123-164.compute-1.amazonaws.com", 8989);
		out = new ObjectOutputStream(socket.getOutputStream());
		in = new ObjectInputStream(socket.getInputStream());
		window = new GameWindow(this);
		this.start();
	}
	
	public boolean send(Message msg) {
		try {
			out.writeObject(msg);
			return true;
		} catch (IOException e) {
			window.addChat("That last message didn't go through.");
			return false;
		}
	}
	public void sendMessage(String msg) {
		send(MessageFactory.getChatMessage(msg));
	}
	public void sendHit() {
		send(MessageFactory.getHitMessage());
	}
	public void sendStay() {
		send(MessageFactory.getStayMessage());
	}
	public void sendBust() {
		send(MessageFactory.getBustMessage());
	}
	public void sendWin() {
		send(MessageFactory.getWinMessage(username));
	}
	public void sendStartRequest() {
		if (state == GameState.ENDED) {
			if (send(MessageFactory.getStartMessage())) {
				state = GameState.REQUESTED_START;
			}
		}
	}
	public void sendJoinRequest() {
		if (state == GameState.STARTED) {
			if (send(MessageFactory.getJoinMessage())) {
				state = GameState.REQUESTED_JOIN;
			}
		}
	}
	public void sendLogin() {
		if (state == GameState.UNAUTHENTICATED) {
			if (send(MessageFactory.getLoginMessage(username))) {
				state = GameState.REQUESTED_LOGIN;
			}
		}
	}
	
	void setUsername(String username) {
		this.username = username;
		sendLogin();
	}
	
	void disconnect(int serverID) {
		try {
			socket.close();
		} catch (IOException e) {
			System.out.println("Unable to close connection");
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		try {
			while (!socket.isClosed()) {
				
				Message msg = (Message) in.readObject();
				MessageType type = msg.getType();
				
				System.out.println("Recieved " + type.name() + " Message");
				
				switch (type) {
					case CARD:
						CardMessage cardMessage = (CardMessage) msg;
						if (cardMessage.getUsername().equals(username)) {
							cards.add(cardMessage.getCard());
							int value = 0;
							String hand = "";
							for (Card card: cards) {
								hand += card.getSuite().name() + card.getValue().name() + ", ";
								switch(card.getValue()) {
								case ACE: //TODO: handle ace different values
								case KING:
								case QUEEN:
								case JACK:
								case TEN:
									value++;
								case NINE:
									value++;
								case EIGHT:
									value++;
								case SEVEN:
									value++;
								case SIX:
									value++;
								case FIVE:
									value++;
								case FOUR:
									value++;
								case THREE:
									value++;
								case TWO:
									value += 2;
									break;
								}
							}
							if (value > 21) {
								sendBust();
							} else if (value == 21) {
								sendWin();
							}
							window.setHand(hand);
						}
						break;
						
					case LOGIN:
						window.addChat(msg.getUsername() + " has logged in.");
						break;
						
					case ACK:
						switch (state) {
						case UNAUTHENTICATED:
							System.out.println("Recieved orphan ACK");
							break;
						case REQUESTED_LOGIN:
							window.addChat("Username was accepted");
							state = GameState.ENDED;
							window.hideLogin();
							break;
						case ENDED:
							System.out.println("Recieved orphan ACK");
							break;
						case JOINED:
							System.out.println("Recieved orphan ACK");
							break;
						case REQUESTED_JOIN:
							window.addChat("Join request was accepted");
							state = GameState.JOINED;
							break;
						case REQUESTED_START:
							window.addChat("Start request was accepted");
							state = GameState.STARTED;
							break;
						case STARTED:
							System.out.println("Recieved orphan ACK");
							break;
						}
						break;
						
					case DENY:
						switch (state) {
						case UNAUTHENTICATED:
							System.out.println("Recieved orphan DENY");
							break;
						case REQUESTED_LOGIN:
							window.addChat("Username was rejected");
							state = GameState.UNAUTHENTICATED;
							window.showLogin();
							break;
						case ENDED:
							System.out.println("Recieved orphan DENY");
							break;
						case JOINED:
							System.out.println("Recieved orphan DENY");
							break;
						case REQUESTED_JOIN:
							window.addChat("Join request was rejected");
							state = GameState.STARTED;
							break;
						case REQUESTED_START:
							window.addChat("Start request was rejected");
							state = GameState.ENDED;
							break;
						case STARTED:
							System.out.println("Recieved orphan DENY");
							break;
						}
						break;
						
					case CHAT:
						ChatMessage chatMessage = (ChatMessage) msg;
						window.addChat(chatMessage.getUsername() + ": " + chatMessage.getText());
						break;
						
					case GAME_STATE:
						GameStateMessage gsMessage = (GameStateMessage) msg;
						switch (gsMessage.getRequestedState()) {
						case JOIN:
							window.addChat("Server requests all join the game.");
							break;
						case START:
							window.addChat("Server has started a game.");
							state = GameState.STARTED;
							break;
						}
						break;
						
					case GAME_ACTION:
						GameActionMessage gaMessage = (GameActionMessage) msg;
						switch (gaMessage.getAction()) {
						case BUST:
							window.addChat("Received BUST for: " + msg.getUsername());
							break;
						case HIT:
							window.addChat("Received HIT for: " + msg.getUsername());
							break;
						case STAY:
							window.addChat("Received STAY for: " + msg.getUsername());
							break;
						case WIN:
							window.addChat("Received WIN for: " + msg.getUsername());
							break;
						}
						break;
				}
			}
			
		} catch (EOFException e) {
			System.out.println("Server has shutdown.");
			window.addChat("Server has shutdown.");
			
		} catch (IOException e) {
			System.out.println("IO Error occured while reading input");
			e.printStackTrace();
			
		} catch (ClassNotFoundException e) {
			System.out.println("Unknown Error occured while reading input");
			e.printStackTrace();
		}
	}
}
