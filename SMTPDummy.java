import java.net.*;
import java.util.*;
import java.io.*;

public class SMTPDummy extends Thread {

	// These commands are responded with a simple OK
	private static final List NOOPS 
		= Arrays.asList(new String[] {"HELO","MAIL","SEND","SOML","RCPT","RSET"});

	// The mbx file
	private static File mbx;
	
	// The socket
	private Socket socket;
	
	// The thread id
	private int id;

	// Internal states
	private static final int COMM_STATE = 0;
	private static final int DATA_STATE = 1;
	private static final int EXIT_STATE = 2;
	
	/** Initializes a receiver thread for an opened socket */
	public SMTPDummy(Socket socket, int id) throws IOException {
		this.socket = socket;
		this.id = id;
	}

	/** Runs the receiver thread **/
	public void run() {
		try {
			
			// Init streams
			BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			OutputStream out = socket.getOutputStream();
			ByteArrayOutputStream message = new ByteArrayOutputStream();
			writeln("220 smtpd(ummy) server", out);
		    String remoteHost = socket.getInetAddress().getHostAddress();
			System.out.println(new Date() + " [" + id + "] - INFO: Accepting connection from " + remoteHost);

			// Init state
			int state = COMM_STATE;
			while (state != EXIT_STATE) {
				
				// Communicate
				while (state == COMM_STATE) {
					String request = in.readLine();
					String cmd = request.length() > 3 ? request.substring(0, 4) : "";
					if (NOOPS.contains(cmd)) {
						writeln("250 OK", out);
					} else if (cmd.equals("DATA")) {
						writeln("354 OK", out);
						writeln("From ???@??? Sun Jan 00 00:00:00 0000", message);
						state = DATA_STATE;
					} else if (cmd.equals("QUIT")) {
						writeln("221 BYE", out);
						state = EXIT_STATE;
					} else {
						writeln("500 WHAT?", out);
					}
				}
				
				// Read data
				while (state == DATA_STATE) {
					String request = in.readLine();
					if (request.equals(".")) {
						writeln("250 OK", out);
						state = COMM_STATE;
					} else {
						writeln(request, message);
					}
				}
				
				// Dump data
				if (message.size() > 0) {
					synchronized (mbx) {
						FileOutputStream file = new FileOutputStream(mbx, true);
						try {
							file.write(message.toByteArray());
							writeln("", file);
							message = new ByteArrayOutputStream();
						} finally {
							file.close();
						}
						System.out.println(new Date() + " [" + id + "] - INFO: Message saved to " + mbx.getAbsolutePath());
					}
				}
			}
		} catch (Exception e) {
			System.out.println(new Date() + " [" + id + "] - WARNING: " + e.getMessage());
		} finally {
			try { 
				socket.close(); 
				System.out.println(new Date() + " [" + id + "] - INFO: Connection closed");
			} catch (Exception e) {}
		}
	}

	/** Writes a line terminated with LF, CR to the output stream**/
	private void writeln(String line, OutputStream out) throws IOException {
		out.write(line.getBytes());
		out.write(new byte[] { 13, 10 });
		out.flush();
	}

	/** Main method takes a filename as argument **/
	public static void main(String[] args) {
		mbx = new File(args.length > 0 ? args[0] : "smtpd(ummy).mbx");
		try {
			ServerSocket socket = new ServerSocket(25);
			System.out.println(new Date() + " [0] - INFO: smtpd(ummy) started on port 25");
			int i = 1;
			while (true) {
				SMTPDummy smtpd = new SMTPDummy(socket.accept(), i++);
				smtpd.start();
			}
		} catch (Exception e) {
			System.out.println(new Date() + " [0] - ERROR: " + e.getMessage());
		}
	}
}
