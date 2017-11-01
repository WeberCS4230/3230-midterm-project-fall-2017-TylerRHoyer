import java.io.*;
import java.net.Socket;
import java.util.Vector;

import blackjack.game.Card;
import blackjack.message.*;
import blackjack.message.GameStateMessage.GameAction;
import blackjack.message.Message.MessageType;
import blackjack.*;

public class Client {
	private Window clientWindow;
	
	private Vector<Socket> sockets;
	private Vector<ObjectOutputStream> out;
	private Vector<Listener> listeners;
	
	public Client() {
		sockets = new Vector<>();
		out = new Vector<>();
		listeners = new Vector<>();
		clientWindow = new Window(this);
	}
	
	
	public void send(int serverID, Message msg) {
		try {
			out.get(serverID).writeObject(msg);
		} catch (IOException e) {
			System.out.println("Unable to send message");
			e.printStackTrace();
		}
	}

	public void sendMessage(int serverID, String msg) {
		send(serverID, MessageFactory.getChatMessage(msg));
	}
	
	public void sendHit(int serverID) {
		send(serverID, MessageFactory.getHitMessage());
	}
	
	public void sendStay(int serverID) {
		send(serverID, MessageFactory.getStayMessage());
	}

	public void sendStart(int serverID) {
		send(serverID, MessageFactory.getStartMessage());
	}
	
	public void sendJoin(int serverID) {
		send(serverID, MessageFactory.getJoinMessage());
	}
	
	
	public void recieve(int serverID, String msg) throws IOException {
		clientWindow.display(serverID, msg);
	}
	
	int connect(String name, String address) throws IOException {
		int serverID = out.size();
		
		Socket newSocket = new Socket(address, 8989);
		sockets.add(newSocket);
		
		out.add(serverID, new ObjectOutputStream(newSocket.getOutputStream()));
		listeners.add(new Listener(this, serverID, new ObjectInputStream(newSocket.getInputStream())));
		return clientWindow.addTab(serverID, address);
	}
	
	public void addCard(int serverID, Message msg) {
		CardMessage c = (CardMessage) msg;
		clientWindow.addToHand(c.getCard());
	}
	
	public void addMessage(int serverID, Message msg) {
		ChatMessage c = (ChatMessage) msg;
		clientWindow.display(serverID, c.getText());
	}

	public void started(int serverID) {
		
	}
	
	public void joined(int serverID) {
		
	}
	
	void disconnect(int serverID) {
		try {
			listeners.get(serverID).disconnect();
			sockets.get(serverID).close();
		} catch (IOException e) {
			System.out.println("Unable to close connection");
			e.printStackTrace();
		}
	}
	
	private class Listener extends Thread {
		private Client parent;
		private ObjectInputStream in;
		private boolean disconnected = false;
		private int serverID;
		
		public Listener(Client parent, int serverID, ObjectInputStream in) {
			this.parent = parent;
			this.in = in;
			this.serverID = serverID;
			start();
		}
		
		public void disconnect() {
			disconnected = true;
		}

		@Override
		public void run() {
			try {
				while (!disconnected) {
					Message msg = (Message) in.readObject();
					MessageType type = msg.getType();
					if (type.equals(MessageType.CARD)) {
						parent.addCard(serverID, msg);
					} else if (type.equals(MessageType.CHAT)) {
						parent.addMessage(serverID, msg);
					} else if (type.equals(MessageType.LOGIN)) {
						System.out.println("Recieved LOGIN");
					} else if (type.equals(MessageType.ACK)) {
						System.out.println("Received ACK");
					} else if (type.equals(MessageType.GAME_STATE)) {
						GameStateMessage s = (GameStateMessage) msg;
						GameAction state = s.getRequestedState();
						if (state == GameAction.START) {
							parent.started(serverID);
						} else if (state == GameAction.JOIN) {
							parent.joined(serverID);
						}
					} else if (type.equals(MessageType.GAME_ACTION)) {
						System.out.println("Recieved game action");
					} else {
						System.out.println("Recieved unknown message type: " + type.toString());
					}
				}
			} catch (IOException e) {
				System.out.println("IO Error occured while reading input");
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				System.out.println("Unknown Error occured while reading input");
				e.printStackTrace();
			}
		}
	}
	
}
