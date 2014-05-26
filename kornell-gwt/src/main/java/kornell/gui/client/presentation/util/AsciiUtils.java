package kornell.gui.client.presentation.util;

public class AsciiUtils {

  /**
   * Contains a list of all the characters that map one to one for UNICODE.
   */
  private static final String PLAIN_ASCII = 
  	      "AaEeIiOoUu"    // grave
  	    + "AaEeIiOoUuYy"  // acute
  	    + "AaEeIiOoUuYy"  // circumflex
  	    + "AaEeIiOoUuYy"  // tilde
  	    + "AaEeIiOoUuYy"  // umlaut
  	    + "Aa"            // ring
  	    + "Cc"            // cedilla
  	    + "Nn"			      // n tilde (spanish)
  	    ;

  /**
   * Actual accented values, corresponds one to one with ASCII
   */
  private static final String UNICODE =
  	 "\u00C0\u00E0\u00C8\u00E8\u00CC\u00EC\u00D2\u00F2\u00D9\u00F9"             
  	+"\u00C1\u00E1\u00C9\u00E9\u00CD\u00ED\u00D3\u00F3\u00DA\u00FA\u00DD\u00FD" 
  	+"\u00C2\u00E2\u00CA\u00EA\u00CE\u00EE\u00D4\u00F4\u00DB\u00FB\u0176\u0177" 
  	+"\u00C2\u00E2\u00CA\u00EA\u00CE\u00EE\u00D4\u00F4\u00DB\u00FB\u0176\u0177" 
  	+"\u00C4\u00E4\u00CB\u00EB\u00CF\u00EF\u00D6\u00F6\u00DC\u00FC\u0178\u00FF" 
  	+"\u00C5\u00E5"                                                             
  	+"\u00C7\u00E7"  
  	+"\u00D1\u00F1"
   ;

  // private constructor, can't be instanciated!
  private AsciiUtils() {		
  }


  public static String convertNonAscii(String s) {

      StringBuilder b = new StringBuilder();

      int n = s.length();
      for (int i = 0; i < n; i++) {
      	char c = s.charAt(i);
      	int pos = UNICODE.indexOf(c);
      	if (pos > -1) {
            b.append(PLAIN_ASCII.charAt(pos));
      	} else {
            b.append(c);
      	}
      }

     return b.toString();

  }

}
