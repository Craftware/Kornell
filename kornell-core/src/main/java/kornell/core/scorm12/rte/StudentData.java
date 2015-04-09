package kornell.core.scorm12.rte;

public class StudentData extends DMElement {
	public static final StudentData dme = new StudentData();
	public static final MasteryScore mastery_score = MasteryScore.dme;
	
	public StudentData() {
		super("student_data");
		add(mastery_score);
	}
}
