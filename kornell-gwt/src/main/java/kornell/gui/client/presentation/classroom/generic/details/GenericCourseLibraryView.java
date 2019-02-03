package kornell.gui.client.presentation.classroom.generic.details;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.gwtbootstrap.client.ui.Icon;
import com.github.gwtbootstrap.client.ui.base.IconAnchor;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.google.web.bindery.event.shared.EventBus;

import kornell.api.client.KornellSession;
import kornell.core.entity.CourseDetailsLibrary;
import kornell.core.util.StringUtils;
import kornell.gui.client.KornellConstants;
import kornell.gui.client.util.forms.FormHelper;

public class GenericCourseLibraryView extends Composite {

    interface MyUiBinder extends UiBinder<Widget, GenericCourseLibraryView> {
    }

    private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);
    private KornellConstants constants = GWT.create(KornellConstants.class);

    @UiField
    FlowPanel libraryPanel;
    @UiField
    FlowPanel titlePanel;
    @UiField
    FlowPanel contentPanel;

    com.github.gwtbootstrap.client.ui.Button btnFile;

    FlowPanel filesPanel;
    FlowPanel filesHeader;
    FlowPanel filesWrapper;

    Button btnIcon;
    Button btnName;
    Button btnSize;
    Button btnPublishingDate;
    Button btnLastClicked;
    private static Integer ORDER_ASCENDING = 0;
    private static Integer ORDER_DESCENDING = 1;
    private Integer order = ORDER_ASCENDING;
    Map<String, FlowPanel> fileWidgetMap;
    private List<CourseDetailsLibrary> courseDetailsLibraries;
    private FormHelper formHelper;

    public GenericCourseLibraryView(EventBus eventBus, KornellSession session, PlaceController placeCtrl,
            List<CourseDetailsLibrary> courseDetailsLibraries) {
        this.courseDetailsLibraries = courseDetailsLibraries;
        this.formHelper = new FormHelper();
        initWidget(uiBinder.createAndBindUi(this));
        initData();
    }

    private void initData() {
        /*
         * client.getCourses(new Callback<CoursesTO>() {
         * 
         * @Override protected void ok(CoursesTO to) { display(); } });
         */
        display();
    }

    private void display() {
        displayTitle();
        fileWidgetMap = new HashMap<String, FlowPanel>();
        contentPanel.add(getFilesTable(btnLastClicked));
        handleEvent(btnLastClicked);
    }

    private void displayTitle() {
        FlowPanel certificationInfo = new FlowPanel();
        certificationInfo.addStyleName("detailsInfo");

        Label infoTitle = new Label(constants.libraryTitle());
        infoTitle.addStyleName("detailsInfoTitle");
        certificationInfo.add(infoTitle);

        Label infoText = new Label(constants.libraryInfo());
        infoText.addStyleName("detailsInfoText");
        certificationInfo.add(infoText);

        titlePanel.add(certificationInfo);
    }

    private FlowPanel getFilesTable(Button btn) {
        FlowPanel filesPanel = new FlowPanel();
        filesPanel.addStyleName("filesPanel");
        filesPanel.add(getHeader());
        filesPanel.add(getFiles(btn));
        return filesPanel;
    }

    private FlowPanel getFiles(Button btn) {
        if (courseDetailsLibraries.size() > 0) {
            if (btn == null) {
                order = ORDER_ASCENDING;
            } else if (btnIcon.equals(btn)) {
                Collections.sort(courseDetailsLibraries, new FileTypeComparator());
                if (btnIcon.equals(btnLastClicked) && ORDER_ASCENDING.equals(order)) {
                    Collections.reverse(courseDetailsLibraries);
                    order = ORDER_DESCENDING;
                } else {
                    order = ORDER_ASCENDING;
                }
            } else if (btnSize.equals(btn)) {
                Collections.sort(courseDetailsLibraries, new FileSizeComparator());
                if (btnSize.equals(btnLastClicked) && ORDER_DESCENDING.equals(order)) {
                    Collections.reverse(courseDetailsLibraries);
                    order = ORDER_ASCENDING;
                } else {
                    order = ORDER_DESCENDING;
                }
            } else if (btnPublishingDate.equals(btn)) {
                Collections.sort(courseDetailsLibraries, new FilePublishingDateComparator());
                if (btnPublishingDate.equals(btnLastClicked) && ORDER_ASCENDING.equals(order)) {
                    Collections.reverse(courseDetailsLibraries);
                    order = ORDER_DESCENDING;
                } else {
                    order = ORDER_ASCENDING;
                }
            } else {
                Collections.sort(courseDetailsLibraries, new FileNameComparator());
                if (btnName.equals(btnLastClicked) && ORDER_ASCENDING.equals(order)) {
                    Collections.reverse(courseDetailsLibraries);
                    order = ORDER_DESCENDING;
                } else {
                    order = ORDER_ASCENDING;
                }
            }
            btnLastClicked = btn != null ? btn : btnLastClicked;
        }

        if (fileWidgetMap.size() <= 0)
            for (CourseDetailsLibrary fileTO : courseDetailsLibraries)
                fileWidgetMap.put(fileTO.getTitle(), getFilePanel(fileTO));

        filesWrapper = new FlowPanel();
        filesWrapper.addStyleName("filesWrapper");
        for (CourseDetailsLibrary fileTO : courseDetailsLibraries) {
            filesWrapper.add(fileWidgetMap.get(fileTO.getTitle()));
        }
        return filesWrapper;
    }

    private FlowPanel getHeader() {
        if (filesHeader != null)
            return filesHeader;

        filesHeader = new FlowPanel();
        filesHeader.addStyleName("filesHeader");

        btnIcon = btnIcon != null ? btnIcon : new Button(constants.libraryEntryIcon());
        displayHeaderButton(btnIcon, "btnIcon", false);
        filesHeader.add(btnIcon);

        btnName = btnName != null ? btnName : new Button(constants.libraryEntryName());
        displayHeaderButton(btnName, "btnName", false);
        filesHeader.add(btnName);

        btnSize = btnSize != null ? btnSize : new Button(constants.libraryEntrySize());
        displayHeaderButton(btnSize, "btnSize", false);
        filesHeader.add(btnSize);

        btnPublishingDate = btnPublishingDate != null ? btnPublishingDate : new Button(constants.libraryEntryDate());
        displayHeaderButton(btnPublishingDate, "btnPublishingDate", false);
        filesHeader.add(btnPublishingDate);

        return filesHeader;
    }

    private void displayHeaderButton(Button btn, String styleName, boolean selected) {
        btn.removeStyleName("btn");
        btn.addStyleName("btnLibraryHeader");
        btn.addStyleName(styleName);
        btn.addStyleName(selected ? "btnAction" : "btnNotSelected");
        btn.addClickHandler(new LibraryHeaderClickHandler());
    }

    private void handleEvent(Button btn) {
        if (btnLastClicked != null) {
            btnLastClicked.removeStyleName("btnAction");
            btnLastClicked.addStyleName("btnNotSelected");
        }
        contentPanel.clear();
        contentPanel.add(getFilesTable(btn));
        if (btn != null) {
            btn.addStyleName("btnAction");
            btn.removeStyleName("btnNotSelected");
        }
        btnLastClicked = btn;
    }

    private FlowPanel getFilePanel(final CourseDetailsLibrary fileTO) {
        FlowPanel fileWrapper = new FlowPanel();
        fileWrapper.addStyleName("fileWrapper");

        Icon fileIcon = new Icon();
        fileIcon.addStyleName("fa " + fileTO.getFontAwesomeClassName());
        fileIcon.addStyleName("fileIcon");
        fileIcon.addStyleName("cursorPointer");

        IconAnchor fileIconAnchor = new IconAnchor();
        fileIconAnchor.add(fileIcon);

        fileWrapper.add(fileIconAnchor);

        fileIconAnchor.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                Window.open(StringUtils.mkurl(fileTO.getPath(), fileTO.getTitle().replaceAll("[ &]+", "_")), "_blank", "");
            }
        });

        FlowPanel pnlFileName = new FlowPanel();
        pnlFileName.addStyleName("pnlFileName");

        Label fileName = new Label(fileTO.getDescription());
        fileName.addStyleName("fileName");
        fileName.addStyleName("cursorPointer");
        pnlFileName.add(fileName);

        fileName.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                Window.open(StringUtils.mkurl(fileTO.getPath(), fileTO.getTitle().replaceAll("[ &]+", "_")), "_blank", "");
            }
        });

        Label fileDescription = new Label(fileTO.getTitle());
        fileDescription.addStyleName("fileDescription");
        pnlFileName.add(fileDescription);

        fileWrapper.add(pnlFileName);

        Label fileSize = new Label(toFileSizeString(fileTO.getSize()));
        fileSize.addStyleName("fileSize");
        fileWrapper.add(fileSize);

        Label publishingDate = new Label(formHelper.dateToString(fileTO.getUploadDate()));
        publishingDate.addStyleName("publishingDate");
        fileWrapper.add(publishingDate);

        FlowPanel clearPanel = new FlowPanel();
        clearPanel.addStyleName("clear");
        fileWrapper.add(clearPanel);

        return fileWrapper;
    }

    private final class LibraryHeaderClickHandler implements ClickHandler {
        public void onClick(ClickEvent event) {
            handleEvent((Button) event.getSource());
        }
    }

    private final class FileNameComparator implements Comparator<CourseDetailsLibrary> {
        @Override
        public int compare(final CourseDetailsLibrary object1, final CourseDetailsLibrary object2) {
            if (object1.getTitle().compareTo(object2.getTitle()) == 0)
                return object1.getDescription().compareTo(object2.getDescription());
            return object1.getTitle().compareTo(object2.getTitle());
        }
    }

    private final class FilePublishingDateComparator implements Comparator<CourseDetailsLibrary> {
        @Override
        public int compare(final CourseDetailsLibrary object1, final CourseDetailsLibrary object2) {
            if (object1.getUploadDate().compareTo(object2.getUploadDate()) == 0)
                return object1.getTitle().compareTo(object2.getTitle());
            return object1.getUploadDate().compareTo(object2.getUploadDate());
        }
    }

    private final class FileSizeComparator implements Comparator<CourseDetailsLibrary> {
        @Override
        public int compare(final CourseDetailsLibrary object1, final CourseDetailsLibrary object2) {
            try {
                String fileSize1 = toFileSizeString(object1.getSize());
                String fileSize2 = toFileSizeString(object2.getSize());
                int ret = 0;
                String[] parts1 = fileSize1.split(" ");
                String[] parts2 = fileSize2.split(" ");
                Integer value1 = Integer.parseInt(parts1[0]);
                String unit1 = parts1[1].toUpperCase();
                Integer value2 = Integer.parseInt(parts2[0]);
                String unit2 = parts2[1].toUpperCase();
                ret = unit1.compareTo(unit2);
                if (ret == 0)
                    ret = value1.compareTo(value2);
                if (ret == 0)
                    return object1.getTitle().compareTo(object2.getTitle());
                return ret;
            } catch (Exception e) {
                return object2.getSize().compareTo(object1.getSize());
            }
        }
    }

    private String toFileSizeString(Integer size) {
        String fileSizeStr = "";
        Integer mbs = Math.round(size / (1000 * 1000));
        Integer kbs = Math.round(size / (1000));
        if (mbs > 1) {
            fileSizeStr = mbs + " MB";
        } else if (kbs > 1) {
            fileSizeStr = kbs + " KB";
        } else {
            fileSizeStr = size + " B";
        }
        return fileSizeStr;
    }

    private final class FileTypeComparator implements Comparator<CourseDetailsLibrary> {
        @Override
        public int compare(final CourseDetailsLibrary object1, final CourseDetailsLibrary object2) {
            String fileType1 = object1.getTitle().substring(object1.getTitle().lastIndexOf('.') + 1);
            String fileType2 = object2.getTitle().substring(object2.getTitle().lastIndexOf('.') + 1);

            if (fileType1.compareTo(fileType2) == 0)
                return object1.getTitle().compareTo(object2.getTitle());
            return fileType1.compareTo(fileType2);
        }
    }

}