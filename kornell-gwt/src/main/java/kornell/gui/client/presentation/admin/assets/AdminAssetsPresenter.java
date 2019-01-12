package kornell.gui.client.presentation.admin.assets;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import com.github.gwtbootstrap.client.ui.constants.AlertType;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.user.client.ui.Widget;
import com.google.web.bindery.event.shared.EventBus;

import kornell.api.client.Callback;
import kornell.api.client.KornellSession;
import kornell.core.entity.CertificateDetails;
import kornell.core.entity.CertificateType;
import kornell.core.entity.Course;
import kornell.core.entity.CourseClass;
import kornell.core.entity.CourseDetailsEntityType;
import kornell.core.entity.CourseVersion;
import kornell.core.entity.EntityFactory;
import kornell.core.entity.LearningEntity;
import kornell.core.error.KornellErrorTO;
import kornell.core.to.CourseDetailsHintsTO;
import kornell.core.to.CourseDetailsLibrariesTO;
import kornell.core.to.CourseDetailsSectionsTO;
import kornell.core.util.StringUtils;
import kornell.gui.client.ViewFactory;
import kornell.gui.client.event.ShowPacifierEvent;
import kornell.gui.client.util.forms.FormHelper;
import kornell.gui.client.util.view.KornellNotification;

public class AdminAssetsPresenter implements AdminAssetsView.Presenter {
    public static final String CERTIFICATE_FILENAME = "certificate-bg.jpg";
    public static final String CERTIFICATE_DESCRIPTION = "Certificado (JPG, 2000x1428 px)";
    public static final String THUMB_DESCRIPTION = "Ícone (JPG, 150x150 px)";
    public static final String IMAGE_JPG = "image/jpg";
    public static final String THUMB_FILENAME = "thumb.jpg";
    public static final String SECTION = "courseDetailsSections";
    public static final String HINT = "courseDetailsHints";
    public static final String LIBRARY = "courseDetailsLibraries";
    public static final String ADD = "add";
    public static final String EDIT = "edit";

    public static final EntityFactory ENTITY_FACTORY = GWT.create(EntityFactory.class);
    Logger logger = Logger.getLogger(AdminAssetsPresenter.class.getName());
    private static AdminAssetsView view;
    FormHelper formHelper;
    private static KornellSession session;
    private static EventBus bus;
    private ViewFactory viewFactory;
    private static LearningEntity entity;
    private CourseDetailsEntityType courseDetailsEntityType;
    public static String entityName;
    private static String entityUUID;
    private Map<String, String> info;
    private static String filePath;
    private static CertificateDetails certificateDetails;
    private CourseDetailsSectionsTO courseDetailsSections;
    private CourseDetailsHintsTO courseDetailsHints;
    private CourseDetailsLibrariesTO courseDetailsLibraries;
    private static String entityType;

    public AdminAssetsPresenter(KornellSession session, EventBus bus, ViewFactory viewFactory) {
        AdminAssetsPresenter.session = session;
        AdminAssetsPresenter.bus = bus;
        this.viewFactory = viewFactory;
        formHelper = new FormHelper();

        init();
    }

    private void init() {
        view = getView();
        view.setPresenter(this);
    }

    @Override
    public Widget asWidget() {
        return view.asWidget();
    }

    private AdminAssetsView getView() {
        return viewFactory.getAdminAssetsView();
    }

    @Override
    public void init(CourseDetailsEntityType courseDetailsEntityType, LearningEntity entity) {
        this.courseDetailsEntityType = courseDetailsEntityType;
        AdminAssetsPresenter.entity = entity;
        AdminAssetsPresenter.entityUUID = entity.getUUID();
        buildViewInfo(courseDetailsEntityType, entity);

        view.initData(courseDetailsEntityType, entityUUID);
        initThumb();
        initCertificateDetails();
        initCourseDetailsSections();
        initCourseDetailsHints();
        initCourseDetailsLibraries();
    }

    private void initThumb() {
        view.initThumb(AdminAssetsPresenter.entity.getThumbUrl() != null);
    }

    private void buildViewInfo(CourseDetailsEntityType courseDetailsEntityType, LearningEntity entity) {
        String thumbSubTitle = null;
        String certificateDetailsSubTitle = null;
        String sectionsSubTitle = " HTML básico é suportado.";
        String hintsSubTitle = " Escolha também o ícone que acompanhará a dica.";
        String librariesSubTitle = null;
        switch (courseDetailsEntityType) {
            case COURSE:
                AdminAssetsPresenter.entityName = "courses";
                AdminAssetsPresenter.entityType = Course.TYPE;
                thumbSubTitle = "Edite o ícone que aparecerá na listagem dos cursos na tela inicial do participante. Observação: a propagação do ícone para todos os participantes pode levar alguns minutos.";
                certificateDetailsSubTitle = "Edite o plano de fundo do certificado para este curso.";
                sectionsSubTitle = "Edite os detalhes da tela de detalhes para este curso." + sectionsSubTitle;
                hintsSubTitle = "Edite as dicas da tela de detalhes para este curso." + hintsSubTitle;
                librariesSubTitle = "Faça o upload dos arquivos da biblioteca do curso.";
                break;
            case COURSE_VERSION:
                AdminAssetsPresenter.entityName = "courseVersions";
                AdminAssetsPresenter.entityType = CourseVersion.TYPE;
                thumbSubTitle = "Edite o ícone que aparecerá na listagem dos cursos na tela inicial do participante. Este ícone será aplicado a todas as turmas desta versão do curso. Observação: a propagação do ícone para todos os participantes pode levar alguns minutos.";
                certificateDetailsSubTitle = "Edite o plano de fundo do certificado para todas as turmas desta versão do curso.";
                sectionsSubTitle = "Edite os detalhes da tela de detalhes para esta versão do curso. Detalhes de uma seção da versão que tenham o título igual a um título de uma seção do curso terão precedência."
                        + sectionsSubTitle;
                hintsSubTitle = "Edite as dicas da tela de detalhes para esta versão do curso. As dicas da versão serão apresentadas após as dicas do curso."
                        + hintsSubTitle;
                librariesSubTitle = "Faça o upload dos arquivos da biblioteca dessa versão do curso. Estes arquivos serão apresentados juntamente com os arquivos do curso.";
                break;
            case COURSE_CLASS:
                AdminAssetsPresenter.entityName = "courseClasses";
                AdminAssetsPresenter.entityType = CourseClass.TYPE;
                thumbSubTitle = "Edite o ícone que aparecerá na listagem dos cursos na tela inicial do participante. Este ícone será aplicado somente a esta turma. Observação: a propagação do ícone para todos os participantes pode levar alguns minutos.";
                certificateDetailsSubTitle = "Edite o plano de fundo do certificado para esta turma.";
                sectionsSubTitle = "Edite os detalhes da tela de detalhes para esta turma. Detalhes de uma seção da turma que tenham o título igual a um título de uma seção do curso ou da versão terão precedência."
                        + sectionsSubTitle;
                hintsSubTitle = "Edite as dicas da tela de detalhes para esta turma. As dicas da versão serão apresentadas após as dicas do curso e da versão do curso."
                        + hintsSubTitle;
                librariesSubTitle = "Faça o upload dos arquivos da biblioteca dessa versão do curso. Estes arquivos serão apresentados juntamente com os arquivos do curso e da versão do curso.";
                break;
        }

        this.info = new HashMap<>();
        info.put("thumbSubTitle", thumbSubTitle);
        info.put("certificateDetailsSubTitle", certificateDetailsSubTitle);
        info.put("sectionsSubTitle", sectionsSubTitle);
        info.put("hintsSubTitle", hintsSubTitle);
        info.put("librariesSubTitle", librariesSubTitle);

        AdminAssetsPresenter.filePath = StringUtils.mkurl(session.getRepositoryAssetsURL(), entityName,
                entity.getUUID());
    }

    private void initCertificateDetails() {
        session.certificatesDetails().findByEntityTypeAndUUID(courseDetailsEntityType, entityUUID,
                new Callback<CertificateDetails>() {
                    @Override
                    public void ok(CertificateDetails to) {
                        certificateDetails = to;
                        view.initCertificateDetails(certificateDetails);
                    }

                    @Override
                    public void notFound(KornellErrorTO kornellErrorTO) {
                        creteNewCertificateDetails();
                        view.initCertificateDetails(certificateDetails);
                    }
                });
    }

    @Override
    public void initCourseDetailsSections() {
        session.courseDetailsSections().findByEntityTypeAndUUID(courseDetailsEntityType, entityUUID,
                new Callback<CourseDetailsSectionsTO>() {
                    @Override
                    public void ok(CourseDetailsSectionsTO to) {
                        courseDetailsSections = to;
                        view.initCourseDetailsSections(courseDetailsSections);
                    }
                });
    }

    @Override
    public void initCourseDetailsHints() {
        session.courseDetailsHints().findByEntityTypeAndUUID(courseDetailsEntityType, entityUUID,
                new Callback<CourseDetailsHintsTO>() {
                    @Override
                    public void ok(CourseDetailsHintsTO to) {
                        courseDetailsHints = to;
                        view.initCourseDetailsHints(courseDetailsHints);
                    }
                });
    }

    @Override
    public void initCourseDetailsLibraries() {
        session.courseDetailsLibraries().findByEntityTypeAndUUID(courseDetailsEntityType, entityUUID,
                new Callback<CourseDetailsLibrariesTO>() {
                    @Override
                    public void ok(CourseDetailsLibrariesTO to) {
                        courseDetailsLibraries = to;
                        view.initCourseDetailsLibraries(courseDetailsLibraries);
                    }
                });
    }

    private void creteNewCertificateDetails() {
        certificateDetails = ENTITY_FACTORY.newCertificateDetails().as();
        certificateDetails.setEntityType(courseDetailsEntityType);
        certificateDetails.setEntityUUID(entityUUID);
        certificateDetails.setCertificateType(CertificateType.DEFAULT);
        certificateDetails.setBgImage(filePath);
    }

    @Override
    public void getUploadURL(final String contentType, final String elementId, final String fileName) {
        session.assets().getUploadURL(entityName, entityUUID, fileName, "", new Callback<String>() {
            @Override
            public void ok(String url) {
                String[] allowedFileExtensions = {"jpg", "jpeg", "png", "gif"};
                getFile(elementId, contentType, url, fileName, allowedFileExtensions);
            }
        });
    }

    public static native void getFile(String elementId, String contentType, String url, String fileName, String[] allowedFileExtensions) /*-{
        if ($wnd.document.getElementById(elementId).files.length != 1) {
            @kornell.gui.client.util.view.KornellNotification::showError(Ljava/lang/String;)("Por favor selecione uma imagem.");
        } else {
            @kornell.gui.client.presentation.admin.assets.AdminAssetsPresenter::showPacifier()();
            var file = $wnd.document.getElementById(elementId).files[0],
                splitFileName = file.name.split(".");

            if (!splitFileName || allowedFileExtensions.indexOf(splitFileName[1]) == -1) {
                @kornell.gui.client.util.view.KornellNotification::showError(Ljava/lang/String;)("Faça o upload de uma imagem do formato exigido.");
                @kornell.gui.client.presentation.admin.assets.AdminAssetsPresenter::hidePacifier()();
            } else {
                var req = new XMLHttpRequest();
                req.open('PUT', url);
                req.setRequestHeader("Content-type", contentType);
                req.setRequestHeader("x-ms-blob-type", "BlockBlob");
                req.onreadystatechange = function () {
                    if (req.readyState == 4 && (req.status == 200 || req.status == 201)) {
                        @kornell.gui.client.presentation.admin.assets.AdminAssetsPresenter::postProcessImageUpload(Ljava/lang/String;)(fileName);
                    } else if (req.readyState != 2) {
                        @kornell.gui.client.presentation.admin.assets.AdminAssetsPresenter::hidePacifier()();
                        @kornell.gui.client.presentation.admin.assets.AdminAssetsPresenter::errorUpdatingImage()();
                    }
                }

                var formData = new FormData();
                formData.append('file', file, fileName);
                req.send(file);
            }
        }
    }-*/;

    public static void errorUpdatingImage() {
        KornellNotification.show("Erro ao atualizar imagem.", AlertType.ERROR);
    }

    public static void showPacifier() {
        bus.fireEvent(new ShowPacifierEvent(true));
    }

    public static void hidePacifier() {
        bus.fireEvent(new ShowPacifierEvent(false));
    }

    public static void postProcessImageUpload(String fileName) {
        if (THUMB_FILENAME.equals(fileName)) {
            updateThumbnail(fileName);
        } else if (CERTIFICATE_FILENAME.equals(fileName)) {
            upsertCertificateDetails();
        }
    }

    @Override
    public void delete(String fileName) {
        if (THUMB_FILENAME.equals(fileName)) {
            deleteThumbnail();
        } else if (CERTIFICATE_FILENAME.equals(fileName)) {
            deleteCertificateDetails();
        }
    }

    private static void updateThumbnail(String fileName) {
        entity.setThumbUrl(StringUtils.mkurl(filePath, fileName));
        view.initThumb(entity.getThumbUrl() != null);
        session.assets().updateThumbnail(entityName, entityUUID, entity, entityType, new Callback<LearningEntity>() {

            @Override
            public void ok(LearningEntity to) {
                hidePacifier();
                KornellNotification.show("Atualização do ícone concluída.");
            }
        });
    }

    private void deleteThumbnail() {
        entity.setThumbUrl(null);
        initThumb();
        session.assets().updateThumbnail(entityName, entityUUID, entity, entityType, null);
    }

    private static void upsertCertificateDetails() {
        if (certificateDetails.getUUID() == null) {
            session.certificatesDetails().create(certificateDetails, new Callback<CertificateDetails>() {
                @Override
                public void ok(CertificateDetails to) {
                    doSuccessUpsertCertificateDetails(to);
                }
            });
        } else {
            session.certificateDetails(certificateDetails.getUUID()).update(certificateDetails,
                    new Callback<CertificateDetails>() {
                        @Override
                        public void ok(CertificateDetails to) {
                            doSuccessUpsertCertificateDetails(to);
                        }
                    });
        }
    }

    private static void doSuccessUpsertCertificateDetails(CertificateDetails to) {
        hidePacifier();
        certificateDetails = to;
        KornellNotification.show("Atualização do certificado concluída.");
        view.initCertificateDetails(certificateDetails);
    }

    private void deleteCertificateDetails() {
        session.certificateDetails(certificateDetails.getUUID()).delete(new Callback<CertificateDetails>() {
            @Override
            public void ok(CertificateDetails to) {
                hidePacifier();
                creteNewCertificateDetails();
                view.initCertificateDetails(certificateDetails);
                KornellNotification.show("O plano de fundo do certificado foi apagado com sucesso.");
            }
        });
    }

    @Override
    public String getFileURL(String fileName) {
        return StringUtils.mkurl(filePath, fileName);
    }

    @Override
    public Map<String, String> getInfo() {
        return info;
    }

    @Override
    public CourseDetailsSectionsTO getCourseDetailsSectionsTO() {
        return courseDetailsSections;
    }

    @Override
    public CourseDetailsHintsTO getCourseDetailsHintsTO() {
        return courseDetailsHints;
    }

    @Override
    public CourseDetailsLibrariesTO getCourseDetailsLibrariesTO() {
        return courseDetailsLibraries;
    }
}
