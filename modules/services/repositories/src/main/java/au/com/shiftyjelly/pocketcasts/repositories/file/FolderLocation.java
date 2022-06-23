package au.com.shiftyjelly.pocketcasts.repositories.file;

public class FolderLocation {
	
	private String filePath;
	private String label;
	
	public FolderLocation(String filePath, String label){
		this.filePath = filePath;
		this.label = label;
	}
	
	public String getFilePath() {
		return filePath;
	}
	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}
	
	public String getLabel() {
		return label;
	}
	public void setLabel(String label) {
		this.label = label;
	}

}
