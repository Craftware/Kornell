package kornell.gui.client.presentation;

import kornell.gui.client.presentation.atividade.AtividadePlace;
import kornell.gui.client.presentation.course.chat.CourseChatPlace;
import kornell.gui.client.presentation.course.course.CourseHomePlace;
import kornell.gui.client.presentation.course.details.CourseDetailsPlace;
import kornell.gui.client.presentation.course.forum.CourseForumPlace;
import kornell.gui.client.presentation.course.library.CourseLibraryPlace;
import kornell.gui.client.presentation.course.notes.CourseNotesPlace;
import kornell.gui.client.presentation.course.specialists.CourseSpecialistsPlace;
import kornell.gui.client.presentation.home.HomePlace;
import kornell.gui.client.presentation.terms.TermsPlace;
import kornell.gui.client.presentation.vitrine.VitrinePlace;
import kornell.gui.client.presentation.welcome.WelcomePlace;

import com.google.gwt.place.shared.PlaceHistoryMapper;
import com.google.gwt.place.shared.WithTokenizers;


@WithTokenizers({HomePlace.Tokenizer.class,
	VitrinePlace.Tokenizer.class,
	TermsPlace.Tokenizer.class,
	WelcomePlace.Tokenizer.class,
	
	CourseHomePlace.Tokenizer.class,
	CourseDetailsPlace.Tokenizer.class,
	CourseLibraryPlace.Tokenizer.class,
	CourseForumPlace.Tokenizer.class,
	CourseChatPlace.Tokenizer.class,
	CourseSpecialistsPlace.Tokenizer.class,
	CourseNotesPlace.Tokenizer.class,
	
	AtividadePlace.Tokenizer.class})
public interface HistoryMapper extends PlaceHistoryMapper {
}
