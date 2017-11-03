import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.JTextComponent;

public class GameWindow extends JFrame {
	
	public GameWindow (Client client) {
		this.client = client;
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		
		loginPanel = new JPanel();
		
			username = new JTextField();
			username.setText("Name");
			username.setPreferredSize(new Dimension(200, 30));
			loginPanel.add(username);
		
			JButton login = new JButton();
			login.setText("Log in");
			login.setPreferredSize(new Dimension(120, 30));
			login.addActionListener(event -> client.setUsername(username.getText()));
			loginPanel.add(login);
		
		chatPanel = new JPanel();
		chatPanel.setLayout(new BorderLayout());
		
			msgPanel = new JPanel();
			msgPanel.setLayout(new BoxLayout(msgPanel, BoxLayout.PAGE_AXIS));
		
			JScrollPane scrollPane = new JScrollPane(msgPanel);
			scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			chatPanel.add(scrollPane, BorderLayout.CENTER);

			JPanel gameMechs = new JPanel();
			gameMechs.setPreferredSize(new Dimension(200, 30));
			chatPanel.add(gameMechs, BorderLayout.NORTH);
			
				hand = new JTextArea();
				hand.setEditable(false);
				hand.setPreferredSize(new Dimension(300, 30));
				hand.setText("empty");
				gameMechs.add(hand);	
				
				JButton hit = new JButton();
				hit.setPreferredSize(new Dimension(75, 30));
				hit.setText("HIT");
				hit.addActionListener(event -> client.sendHit());
				gameMechs.add(hit);
				
				JButton stay = new JButton();
				stay.setPreferredSize(new Dimension(75, 30));
				stay.setText("STAY");
				stay.addActionListener(event -> client.sendStay());
				gameMechs.add(stay);
				
				JButton start = new JButton();
				start.setPreferredSize(new Dimension(75, 30));
				start.setText("START");
				start.addActionListener(event -> client.sendStartRequest());
				gameMechs.add(start);
		
				JButton join = new JButton();
				join.setPreferredSize(new Dimension(75, 30));
				join.setText("JOIN");
				join.addActionListener(event -> client.sendJoinRequest());
				gameMechs.add(join);
				
		messagePanel = new JPanel();
		messagePanel.setLayout(new FlowLayout());
		messagePanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("control ENTER"), "Toggle");
		messagePanel.getActionMap().put("Toggle", new ToggleChat());
		
			messageField = new JTextField();
			messageField.setPreferredSize(new Dimension(525, 30));
			messagePanel.add(messageField);
			
			messageArea = new JTextArea();
			messageArea.setPreferredSize(new Dimension(525, 300));
			
			sendButton = new JButton();
			sendButton.setPreferredSize(new Dimension(75, 30));
			sendButton.setText("Send");
			sendButton.addActionListener(event -> flushChat());
			messagePanel.add(sendButton);
		
		showLogin();
		pack();
		revalidate();
		setVisible(true);
	}
	
	void hideLogin(){
		remove(loginPanel);
		add(chatPanel);
		add(messagePanel, BorderLayout.SOUTH);
		
		pack();
		revalidate();
		repaint();
	}
	
	void showLogin(){
		remove(chatPanel);
		remove(messagePanel);
		add(loginPanel);
		
		pack();
		revalidate();
		repaint();
	}
	
	void setHand(String txt) {
		hand.setText(txt);
	}
	
	void addChat(String msg) {
		JLabel msgLabel = new JLabel();
		msgLabel.setText(msg);
		msgLabel.setPreferredSize(new Dimension(650, 30));
		msgPanel.add(msgLabel);

		pack();
		revalidate();
		repaint();
	}
	
	private void flushChat() {
		client.sendMessage(isChatExpanded ? messageArea.getText() : messageField.getText());
		messageArea.setText("");
		messageField.setText("");
	}
	
	private class ToggleChat extends AbstractAction {
		private static final long serialVersionUID = 1L;
		private final Dimension expandedSize = new Dimension(600, 300);
		private final Dimension unexpandedSize = new Dimension(600, 570);
		
		public void actionPerformed(ActionEvent arg0) {
			isChatExpanded = !isChatExpanded;
			JTextComponent adding;
			JTextComponent removing;
			Dimension size;
			
			if (isChatExpanded) {
				adding = messageArea;
				removing = messageField;
				size = expandedSize;
			} else {
				adding = messageField;
				removing = messageArea;
				size = unexpandedSize;
			}
			
			chatPanel.setPreferredSize(size);
			
			messagePanel.remove(removing);
			messagePanel.remove(sendButton);
			
			messagePanel.add(adding);
			messagePanel.add(sendButton);
			
			adding.setText(removing.getText());
			adding.grabFocus();

			pack();
			revalidate();
			repaint();
		}
	}
	
	private static final long serialVersionUID = 1L;
	
	private JPanel loginPanel;
	private JPanel chatPanel;
	private JPanel messagePanel;
	private JPanel msgPanel;
	
	private JTextField username;
	private JTextArea hand;
	private JTextField messageField;
	private JTextArea messageArea;
	private JButton sendButton;
	
	private boolean isChatExpanded = false;
	
	private Client client;
	
}
