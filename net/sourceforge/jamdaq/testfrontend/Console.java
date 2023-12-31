package net.sourceforge.jamdaq.testfrontend;

import jam.global.JamException;
import jam.global.MessageHandler;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

/**
 * Class Console displays a output of commands and error messages and allows the
 * input of commands using the keyboard.
 * 
 * @author Ken Swartz
 * @author Dale Visser
 * @version 1.2 alpha last edit 15 Feb 2000
 * @version 0.5 last edit 11-98
 * @version 0.5 last edit 1-99
 */
public class Console extends JPanel implements MessageHandler {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final static int DISPLAY_LINES = 25;
	private final static int NUMBER_LINES_LOG = 1000;

	public static final Logger LOGGER = Logger.getLogger(Console.class
			.getPackage().getName());

	static {
		try {
			LOGGER.addHandler(new FileHandler());
		} catch (final IOException ioe) {
			System.err.println(ioe.getMessage());// NOPMD
		}
	}

	/**
	 * End of line character(s).
	 */
	private static final String END_LINE = System
			.getProperty("line.separator");

	private transient final JTextPane textLog; // output text area
	private transient final Document doc;
	private transient final SimpleAttributeSet attr_normal, attr_warning, attr_error;
	// Is the message a new one or a continuation of one
	private transient boolean msgLock;
	// a lock for message output so message don't overlap
	/**
	 * Private.
	 * 
	 * @serial
	 */
	private transient final int maxLines;
	private transient int numberLines; // number of lines in output

	private transient final Object syncLock = new Object();

	/**
	 * Private.
	 * 
	 * @serial
	 */
	private transient BufferedWriter logFileWriter; // output stream
	/**
	 * Private.
	 * 
	 * @serial
	 */
	private transient boolean logFileOn; // are we logging to a file
	/**
	 * Private.
	 * 
	 * @serial
	 */
	private transient String messageFile; // message for file

	/**
	 * Create a JamConsole which has an text area for output a text field for
	 * intput.
	 */
	public Console() {
		this(NUMBER_LINES_LOG);
	}

	/**
	 * Constructor: Create a JamConsole which has an text area for output a text
	 * field for input.
	 */
	public Console(final int linesLog) {
		super(new BorderLayout(5, 5));
		maxLines = linesLog;
		textLog = new JTextPane();
		doc = textLog.getStyledDocument();
		attr_normal = new SimpleAttributeSet();
		attr_warning = new SimpleAttributeSet();
		StyleConstants.setForeground(attr_warning, Color.blue);
		attr_error = new SimpleAttributeSet();
		StyleConstants.setForeground(attr_error, Color.red);
		textLog.setEditable(false);
		final JScrollPane jsp = new JScrollPane(textLog,
				ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		this.add(jsp, BorderLayout.CENTER);
		msgLock = false;
		numberLines = 1;
		logFileOn = false;
		this.setPreferredSize(new Dimension(800, 28 + 16 * DISPLAY_LINES));

	}

	/**
	 * Outputs the string as a message to the console, which has more than one
	 * part, so message can continued by a subsequent call.
	 * 
	 * @param _message
	 *            the message to be output
	 * @param part
	 *            one of NEW, CONTINUE, or END
	 */
	public void messageOut(final String _message, final int part) {
		synchronized (syncLock) {
			String message = _message;
			if (part == NEW) {
				msgLock = true;
				messageFile = getDate() + ">" + message;
				message = END_LINE + getTime() + ">" + message;
				try {
					doc.insertString(doc.getLength(), message, attr_normal);
				} catch (final BadLocationException e) {
					logException("messageOut", e);
				}
			} else if (part == CONTINUE) {
				messageFile = messageFile + message;
				try {
					doc.insertString(doc.getLength(), message, attr_normal);
				} catch (final BadLocationException e) {
					logException("messageOut", e);
				}
			} else if (part == END) {
				messageFile = messageFile + message + END_LINE;
				try {
					doc.insertString(doc.getLength(), message, attr_normal);
				} catch (final BadLocationException e) {
					logException("messageOut", e);
				}
				trimLog();
				textLog.setCaretPosition(doc.getLength());
				// if file logging on write to file
				if (logFileOn) {
					try {
						logFileWriter.write(messageFile, 0, messageFile
								.length());
						logFileWriter.flush();
					} catch (final IOException ioe) {
						logFileOn = false;
						errorOutln("Unable to write to log file, logging turned off [JamConsole]");
					}
				}
				// unlock text area and notify others they can use it
				msgLock = false;
				notifyAll();
			} else {
				LOGGER.severe("Error not a valid message part [JamConsole]");
			}
		}
	}

	private void logException(final String method, final Throwable throwing) {
		LOGGER.throwing("Console", method, throwing);
	}

	/**
	 * Output a message so it will be continued on the same line.
	 */
	public void messageOut(final String message) {
		messageOut(message, CONTINUE);
	}

	/**
	 * Output a message with a carriage return.
	 * 
	 * @param _message
	 *            the message to be printed to the console
	 */
	public void messageOutln(final String _message) {
		synchronized (syncLock) {
			String message = _message;
			msgLock = true;
			messageFile = getDate() + ">" + message + END_LINE;
			message = END_LINE + getTime() + ">" + message;
			try {
				doc.insertString(doc.getLength(), message, attr_normal);
			} catch (final BadLocationException e) {
				logException("messageOutln", e);
			}
			trimLog();
			textLog.setCaretPosition(doc.getLength());
			// if file logging on write to file
			if (logFileOn) {
				try {
					logFileWriter.write(messageFile, 0, messageFile.length());
					logFileWriter.flush();
				} catch (final IOException ioe) {
					logFileOn = false;
					errorOutln("Unable to write to log file, logging turned off [JamConsole]");
				}
			}
			// unlock text area and notify others they can use it
			msgLock = false;
		}
	}

	/**
	 * Writes an error message to the console immediately.
	 */
	public void errorOutln(final String message) {
		LOGGER.severe(message);
		promptOutln("Error: " + message, attr_error);
	}

	/**
	 * Outputs a warning message to the console immediately.
	 */
	public void warningOutln(final String message) {
		promptOutln("Warning: " + message, attr_warning);
	}

	private void promptOutln(final String _message, final AttributeSet attr) {
		synchronized (syncLock) {
			String message = _message;
			/*
			 * Dont wait for lock. Output message right away.
			 */
			if (msgLock) { // if locked add extra returns
				messageFile = END_LINE + getDate() + ">" + message + END_LINE;
				message = END_LINE + getTime() + ">" + message + END_LINE;
			} else { // normal message
				messageFile = getDate() + ">" + message + END_LINE;
				message = END_LINE + getTime() + ">" + message;
			}
			try {
				doc.insertString(doc.getLength(), message, attr);
			} catch (final BadLocationException e) {
				logException("promptOutln", e);
			}
			trimLog();
			textLog.setCaretPosition(doc.getLength());
			/* beep */
			Toolkit.getDefaultToolkit().beep();
			if (logFileOn) { // if file logging on write to file
				try {
					logFileWriter.write(messageFile, 0, messageFile.length());
					logFileWriter.flush();
				} catch (final IOException ioe) {
					logFileOn = false;
					errorOutln("Unable to write to log file, logging turned off [JamConsole]");
				}
			}
		}
	}

	public static final String INTS_ONLY = "int";

	/**
	 * Create a file for the log to be saved to. The method appends a number
	 * (starting at 1) to the file name if the file already exists.
	 * 
	 * @exception JamException
	 *                exceptions that go to the console
	 */
	public String setLogFileName(final String name) {
		File file;
		String newName;
		int fileIndex;

		newName = name + ".log";
		file = new File(newName);
		// create a unique file, append a number if a log already exits.
		fileIndex = 1;
		while (file.exists()) {
			newName = name + fileIndex + ".log";
			file = new File(newName);// NOPMD
			fileIndex++;
		}
		// create a new logFileWriter
		try {
			logFileWriter = new BufferedWriter(new FileWriter(file));

		} catch (final IOException ioe) {
			errorOutln("Not able to create log file " + newName);
		}
		return newName;
	}

	/**
	 * Close the log file
	 * 
	 * @exception JamException
	 *                exceptions that go to the console
	 */
	public void closeLogFile() {
		try {
			logFileWriter.flush();
			logFileWriter.close();
		} catch (final IOException ioe) {
			errorOutln("Could not close log file  [JamConsole]");
		}
	}

	/**
	 * Turn on the logging to a file
	 * 
	 * @exception JamException
	 *                exceptions that go to the console
	 */
	public void setLogFileOn(final boolean state) {
		if (null == logFileWriter) {
			logFileOn = false;
			errorOutln("Cannot turn on logging to file, log file does not exits  [JamConsole]");
		} else {
			logFileOn = state;
		}
	}

	/**
	 * Trim the text on screen Log so it does not get too long
	 */
	private void trimLog() {
		numberLines++;
		if (numberLines > maxLines) { // get rid of top line
			numberLines--;
			try {
				doc.remove(0, textLog.getText().indexOf(END_LINE)
						+ END_LINE.length());
			} catch (final BadLocationException ble) {
				logException("trimLog", ble);
			}
		}
	}

	/**
	 * get the current time
	 */
	private String getTime() {
		Date date;
		DateFormat datef;
		String stime;

		date = new java.util.Date(); // get time
		datef = DateFormat.getTimeInstance(DateFormat.MEDIUM);
		// medium time format
		datef.setTimeZone(TimeZone.getDefault()); // set time zone
		stime = datef.format(date); // format time
		return stime;
	}

	/**
	 * Get the current date and time
	 */
	private String getDate() {
		Date date; // date object
		DateFormat datef;
		String stime;

		date = new java.util.Date(); // get time
		datef = DateFormat.getDateTimeInstance(); // medium date time format
		datef.setTimeZone(TimeZone.getDefault()); // set time zone
		stime = datef.format(date); // format time
		return stime;
	}

	/**
	 * On a class destruction close log file
	 */
	@Override
	protected void finalize() throws Throwable {
		try {
			if (logFileOn) {
				closeLogFile();
			}
		} finally {
			super.finalize();
		}
	}

	public void messageOutln() {
		this.messageOutln("");
	}
}
