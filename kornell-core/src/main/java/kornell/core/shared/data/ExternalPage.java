package kornell.core.shared.data;

public interface ExternalPage extends Actom {
	String getTitle();
	void setTitle(String title);
	
	String getURL();
	void setURL(String url);
}