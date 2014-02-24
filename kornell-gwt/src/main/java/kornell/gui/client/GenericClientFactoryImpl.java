package kornell.gui.client;

import java.util.logging.Logger;

import kornell.api.client.Callback;
import kornell.api.client.KornellSession;
import kornell.core.entity.EntityFactory;
import kornell.core.entity.Institution;
import kornell.core.event.EventFactory;
import kornell.core.lom.LOMFactory;
import kornell.core.to.CourseClassTO;
import kornell.core.to.CourseClassesTO;
import kornell.core.to.TOFactory;
import kornell.gui.client.personnel.Captain;
import kornell.gui.client.personnel.Dean;
import kornell.gui.client.personnel.Stalker;
import kornell.gui.client.presentation.GlobalActivityMapper;
import kornell.gui.client.presentation.HistoryMapper;
import kornell.gui.client.presentation.admin.home.AdminHomePlace;
import kornell.gui.client.presentation.course.ClassroomPlace;
import kornell.gui.client.presentation.vitrine.VitrinePlace;
import kornell.gui.client.util.ClientProperties;
import kornell.scorm.client.scorm12.SCORM12Adapter;
import kornell.scorm.client.scorm12.SCORM12Binder;

import com.google.gwt.activity.shared.ActivityManager;
import com.google.gwt.core.client.GWT;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.place.shared.PlaceHistoryHandler;
import com.google.gwt.user.client.Window;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.SimpleEventBus;

//TODO: Organize this big, messy class and interface
public class GenericClientFactoryImpl implements ClientFactory {
	Logger logger = Logger.getLogger(GenericClientFactoryImpl.class.getName());

	public static final EntityFactory entityFactory = GWT.create(EntityFactory.class);
	public static final TOFactory toFactory = GWT.create(TOFactory.class);
	public static final LOMFactory lomFactory = GWT.create(LOMFactory.class);
	public static final EventFactory eventFactory = GWT.create(EventFactory.class);

	/* History Management */
	private final EventBus bus = new SimpleEventBus();
	private final PlaceController placeCtrl = new PlaceController(bus);
	private final HistoryMapper historyMapper = GWT.create(HistoryMapper.class);
	private final PlaceHistoryHandler historyHandler = new PlaceHistoryHandler(historyMapper);

	/* Activity Managers */
	private ActivityManager globalActivityManager;

	/* GUI */
	private ViewFactory viewFactory;
	private Place defaultPlace;
	private KornellSession session = new KornellSession();
	

	public GenericClientFactoryImpl() {
	}

	private void initActivityManagers() {
		initGlobalActivityManager();
	}

	private void initGlobalActivityManager() {
		globalActivityManager = new ActivityManager(new GlobalActivityMapper(this), bus);
		globalActivityManager.setDisplay(viewFactory.getShell());
	}

	private void initHistoryHandler(Place defaultPlace) {
		historyHandler.register(placeCtrl, bus, defaultPlace);
		historyHandler.handleCurrentHistory();
		if (!session.isAuthenticated())
			placeCtrl.goTo(defaultPlace);
	}

	@Override
	public void startApp() {
		final Callback<CourseClassesTO> courseClassesCallback = new Callback<CourseClassesTO>() {
			@Override
			public void ok(final CourseClassesTO courseClasses) {
				CourseClassTO courseClassTO = null;
				for (CourseClassTO courseClassTmp : courseClasses.getCourseClasses()) {
					if (courseClassTmp.getCourseClass().getInstitutionUUID().equals(Dean.getInstance().getInstitution().getUUID())) {
						courseClassTO = courseClassTmp;
					}
				}
				if(courseClassTO != null){
					Dean.getInstance().setCourseClassTO(courseClassTO);
					setDefaultPlace(new ClassroomPlace(courseClassTO.getEnrollment().getUUID()));
					startAuthenticated(session);
				} else {
					setDefaultPlace(new VitrinePlace());
					startAnonymous(session);
				}
			}
		};
		
		final Callback<Institution> institutionCallback = new Callback<Institution>() {
			@Override
			public void ok(final Institution institution) {
				Dean.init(session, bus, institution);
				if (session.isAuthenticated()) {
					session.getCourseClassesTO(courseClassesCallback);
				} else {
					startAnonymous(session);
				}
			}
			
			@Override
			public void unauthorized(){
				startAnonymous(session);
			}
		};		
		
		session.getInstitutionByName(getInstitutionNameFromLocation(), institutionCallback);

	}
	
	private String getInstitutionNameFromLocation() {
		String institutionName = Window.Location.getParameter("institution");
		if (institutionName == null) {
			institutionName = Window.Location.getHostName().split("\\.")[0];
		}
		return institutionName;
	}

	private void startAnonymous(KornellSession session) {
		ClientProperties.remove("X-KNL-A");
		defaultPlace = new VitrinePlace();
		startClient();
	}

	private void startAuthenticated(KornellSession session) {
		if (session.isCourseClassAdmin()) {
			defaultPlace = new AdminHomePlace();
			startClient();
		} else {
			startClient();
		}
	}

	protected void startClient() {
		initGUI();
		initActivityManagers();
		initHistoryHandler(defaultPlace);
		initException();
		initSCORM12();
		initPersonnel();
	}

	private void initGUI() {
		viewFactory = new GenericViewFactoryImpl(this);
		viewFactory.initGUI();
	}

	private void initPersonnel() {
		new Captain(bus, session, placeCtrl);
		new Stalker(bus, session);
	}

	private void initSCORM12() {				
		SCORM12Binder.bind(new SCORM12Adapter(bus,session));
	}

	private void initException() {
		GWT.setUncaughtExceptionHandler(new GWT.UncaughtExceptionHandler() {
			@Override
			public void onUncaughtException(Throwable e) {
				System.out.println("** UNCAUGHT **");
				e.printStackTrace();
			}
		});
	}

	@Override
	public PlaceController getPlaceController() {
		return placeCtrl;
	}

	@Override
	public HistoryMapper getHistoryMapper() {
		return historyMapper;
	}

	@Override
	public EventBus getEventBus() {
		return bus;
	}

	@Override
	public Place getDefaultPlace() {
		return defaultPlace;
	}

	@Override
	public void setDefaultPlace(Place place) {
		this.defaultPlace = place;
	}

	@Override
	public EntityFactory getEntityFactory() {
		return entityFactory;
	}

	@Override
	public TOFactory getTOFactory() {
		return toFactory;
	}

	@Override
	public LOMFactory getLOMFactory() {
		return lomFactory;
	}

	@Override
	public EventFactory getEventFactory() {
		return eventFactory;
	}



	@Override
	public ViewFactory getViewFactory() {
		return viewFactory;
	}

	@Override
	public void logState() {
		//TODO
	}

	@Override
	public KornellSession getKornellSession() {
		return session;
	}
}