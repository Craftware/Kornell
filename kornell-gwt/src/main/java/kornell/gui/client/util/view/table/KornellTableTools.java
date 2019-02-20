package kornell.gui.client.util.view.table;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.ListBox;
import com.github.gwtbootstrap.client.ui.TextBox;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.github.gwtbootstrap.client.ui.resources.ButtonSize;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.FlowPanel;

import kornell.gui.client.util.AsciiUtils;

public class KornellTableTools<T> extends FlowPanel {

    private PaginationPresenter<T> presenter;
    private Timer updateTimer;
    private TextBox txtSearch;
    private Button btnSearch;
    private ListBox pageSizeListBox;

    public KornellTableTools(PaginationPresenter<T> presenter) {
        this.presenter = presenter;

        updateTimer = new Timer() {
            @Override
            public void run() {
                filter();
            }
        };

        this.addStyleName("marginTop25");
        initSearch();
        initPageSizeListBox();
        refresh();
    }

    private void initPageSizeListBox() {
        this.pageSizeListBox = new ListBox();
        pageSizeListBox.addStyleName("pageSizeListBox");
        pageSizeListBox.addItem("20");
        pageSizeListBox.addItem("50");
        pageSizeListBox.addItem("100");
        pageSizeListBox.setSelectedValue(presenter.getPageSize());
        pageSizeListBox.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent event) {
                if (pageSizeListBox.getValue().matches("[0-9]*")) {
                    presenter.setPageNumber("1");
                    presenter.setPageSize(pageSizeListBox.getValue());
                    presenter.updateData();
                }
            }
        });
        this.add(pageSizeListBox);
    }

    private TextBox initSearch() {
        if (txtSearch == null) {
            txtSearch = new TextBox();
            txtSearch.addStyleName("txtSearch");
            txtSearch.addChangeHandler(new ChangeHandler() {
                @Override
                public void onChange(ChangeEvent event) {
                    scheduleFilter();
                }
            });
            txtSearch.addKeyUpHandler(new KeyUpHandler() {
                @Override
                public void onKeyUp(KeyUpEvent event) {
                    scheduleFilter();
                }
            });
            txtSearch.addValueChangeHandler(new ValueChangeHandler<String>() {

                @Override
                public void onValueChange(ValueChangeEvent<String> event) {
                    scheduleFilter();

                }
            });
            btnSearch = new Button("Pesquisar");
            btnSearch.setSize(ButtonSize.MINI);
            btnSearch.setIcon(IconType.SEARCH);
            btnSearch.addStyleName("btnNotSelected btnSearch");
            btnSearch.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    scheduleFilter();
                }
            });

            this.add(txtSearch);
            this.add(btnSearch);
        }

        txtSearch.setValue(presenter.getSearchTerm());
        txtSearch.setTitle("insira o nome ou o código do conteúdo");
        return txtSearch;
    }

    private void scheduleFilter() {
        updateTimer.cancel();
        updateTimer.schedule(500);
    }

    private void filter() {
        String newSearchTerm = AsciiUtils.convertNonAscii(txtSearch.getText().trim()).toLowerCase();
        if (!presenter.getSearchTerm().equals(newSearchTerm)) {
            presenter.setPageNumber("1");
            presenter.setSearchTerm(newSearchTerm);
            presenter.updateData();
        }
    }

    public String getSearchTerm() {
        return txtSearch.getText();
    }

    public void refresh() {
        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                txtSearch.setFocus(true);
            }
        });

        pageSizeListBox.setVisible(presenter.getTotalRowCount() > 20);
        txtSearch.setText(presenter.getSearchTerm());
    }

    public void resetSearchTerm() {
        if (txtSearch != null) {
            txtSearch.setText("");
            presenter.setSearchTerm("");
        }
    }

}
