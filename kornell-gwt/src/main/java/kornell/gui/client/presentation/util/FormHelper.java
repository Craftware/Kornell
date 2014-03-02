package kornell.gui.client.presentation.util;

import java.util.Date;
import java.util.List;

import kornell.gui.client.uidget.formfield.KornellFormFieldWrapper;
import kornell.gui.client.uidget.formfield.TextBoxFormField;

import com.github.gwtbootstrap.client.ui.ListBox;
import com.github.gwtbootstrap.client.ui.TextBox;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.DefaultDateTimeFormatInfo;

//TODO i18n
public class FormHelper {

	private final String EMAIL_PATTERN = "^[^0-9][A-z0-9_+]+([.][A-z0-9_]+)*[@][A-z0-9_]+([.][A-z0-9_]+)*[.][A-z]{2,4}$";
	private final String USERNAME_PATTERN = "^[^0-9.][A-z0-9.]{2,}$";
	private final String PASSWORD_PATTERN = "^[0-9a-zA-Z!@#$%¨&*()]{6,}$";
	
	public boolean isEmailValid(String field){
		return field == null ? false : field.trim().matches(EMAIL_PATTERN);
	}
	
	public boolean isUsernameValid(String field){
		return field == null ? false : field.trim().matches(USERNAME_PATTERN);
	}
	
	public boolean isPasswordValid(String field){
		return field == null ? false : field.trim().matches(PASSWORD_PATTERN);
	}
	
	public boolean isLengthValid(String field, int minLength, int maxLength){
		return field == null ? false : field.trim().length() >= minLength && field.trim().length() <= maxLength;
	}
	
	public boolean isLengthValid(String field, int minLength){
		return isLengthValid(field, minLength, Integer.MAX_VALUE);
	}
	
	public ListBox getCountriesList(){
		ListBox countries = new ListBox();
		countries.addItem("Selecione:", "-");
		countries.addItem("Afeganistão", "AF");
		countries.addItem("África do Sul", "ZA");
		countries.addItem("Alanda", "AX");
		countries.addItem("Albânia", "AL");
		countries.addItem("Alemanha", "DE");
		countries.addItem("Andorra", "AD");
		countries.addItem("Angola", "AO");
		countries.addItem("Anguilla", "AI");
		countries.addItem("Antártida", "AQ");
		countries.addItem("Antígua e Barbuda", "AG");
		countries.addItem("Arábia Saudita", "SA");
		countries.addItem("Argélia", "DZ");
		countries.addItem("Argentina", "AR");
		countries.addItem("Armênia", "AM");
		countries.addItem("Aruba", "AW");
		countries.addItem("Austrália", "AU");
		countries.addItem("Áustria", "AT");
		countries.addItem("Azerbaijão", "AZ");
		countries.addItem("Bahamas", "BS");
		countries.addItem("Bahrain", "BH");
		countries.addItem("Bangladesh", "BD");
		countries.addItem("Barbados", "BB");
		countries.addItem("Bélgica", "BE");
		countries.addItem("Belize", "BZ");
		countries.addItem("Benin", "BJ");
		countries.addItem("Bermudas", "BM");
		countries.addItem("Bielo-Rússia", "BY");
		countries.addItem("Bolívia", "BO");
		countries.addItem("Bonaire", "BQ");
		countries.addItem("Bósnia-Herzegóvina", "BA");
		countries.addItem("Botsuana", "BW");
		countries.addItem("Brasil", "BR");
		countries.addItem("Brunei", "BN");
		countries.addItem("Bulgária", "BG");
		countries.addItem("Burkina Fasso", "BF");
		countries.addItem("Burundi", "BI");
		countries.addItem("Butão", "BT");
		countries.addItem("Cabo Verde", "CV");
		countries.addItem("Camarões", "CM");
		countries.addItem("Camboja", "KH");
		countries.addItem("Canadá", "CA");
		countries.addItem("Catar", "QA");
		countries.addItem("Cazaquistão", "KZ");
		countries.addItem("Chade", "TD");
		countries.addItem("Chile", "CL");
		countries.addItem("China", "CN");
		countries.addItem("Chipre", "CY");
		countries.addItem("Cingapura", "SG");
		countries.addItem("Colômbia", "CO");
		countries.addItem("Congo", "CG");
		countries.addItem("Congo-Kinshasa", "CD");
		countries.addItem("Coréia, República da", "KR");
		countries.addItem("Coréia, República Popular Democrática da", "KP");
		countries.addItem("Costa do Marfim", "CI");
		countries.addItem("Costa Rica", "CR");
		countries.addItem("Croácia", "HR");
		countries.addItem("Cuba", "CU");
		countries.addItem("Curaçao", "CW");
		countries.addItem("Dinamarca", "DK");
		countries.addItem("Djibuti", "DJ");
		countries.addItem("Dominica", "DM");
		countries.addItem("Egito", "EG");
		countries.addItem("El Salvador", "SV");
		countries.addItem("Emirados Árabes Unidos", "AE");
		countries.addItem("Equador", "EC");
		countries.addItem("Eritreia", "ER");
		countries.addItem("Eslováquia (República Eslovaca)", "SK");
		countries.addItem("Eslovênia", "SI");
		countries.addItem("Espanha", "ES");
		countries.addItem("Estados Unidos", "US");
		countries.addItem("Estônia", "EE");
		countries.addItem("Etiópia", "ET");
		countries.addItem("Fiji", "FJ");
		countries.addItem("Filipinas", "PH");
		countries.addItem("Finlândia", "FI");
		countries.addItem("França", "FR");
		countries.addItem("Gabão", "GA");
		countries.addItem("Gâmbia", "GM");
		countries.addItem("Gana", "GH");
		countries.addItem("Geórgia do Sul e Ilhas Sandwich do Sul", "GS");
		countries.addItem("Geórgia", "GE");
		countries.addItem("Gibraltar", "GI");
		countries.addItem("Granada", "GD");
		countries.addItem("Grécia", "GR");
		countries.addItem("Gronelândia", "GL");
		countries.addItem("Guadalupe", "GP");
		countries.addItem("Guam", "GU");
		countries.addItem("Guatemala", "GT");
		countries.addItem("Guernsey", "GG");
		countries.addItem("Guiana Francesa", "GF");
		countries.addItem("Guiana", "GY");
		countries.addItem("Guiné Equatorial", "GQ");
		countries.addItem("Guiné", "GN");
		countries.addItem("Haiti", "HT");
		countries.addItem("Honduras", "HN");
		countries.addItem("Hong Kong", "HK");
		countries.addItem("Hungria", "HU");
		countries.addItem("Iêmen", "YE");
		countries.addItem("Ilha Bouvet", "BV");
		countries.addItem("Ilha de Man", "IM");
		countries.addItem("Ilha Heard e Ilhas McDonald", "HM");
		countries.addItem("Ilha Norfolk", "NF");
		countries.addItem("Ilhas Caiman", "KY");
		countries.addItem("Ilhas Coco", "CC");
		countries.addItem("Ilhas Comores", "KM");
		countries.addItem("Ilhas Cook", "CK");
		countries.addItem("Ilhas Faroe", "FO");
		countries.addItem("Ilhas Malvinas", "FK");
		countries.addItem("Ilhas Marianas do Norte", "MP");
		countries.addItem("Ilhas Marshall", "MH");
		countries.addItem("Ilhas Menores Distantes dos Estados Unidos", "UM");
		countries.addItem("Ilhas Natal", "CX");
		countries.addItem("Ilhas Salomão", "SB");
		countries.addItem("Ilhas Seychelles", "SC");
		countries.addItem("Ilhas Turks e Caicos", "TC");
		countries.addItem("Ilhas Virgens Britânicas", "VG");
		countries.addItem("Ilhas Virgens dos EUA", "VI");
		countries.addItem("Ilhas Wallis e Futuna", "WF");
		countries.addItem("Índia", "IN");
		countries.addItem("Indonésia", "ID");
		countries.addItem("Irã", "IR");
		countries.addItem("Iraque", "IQ");
		countries.addItem("Irlanda", "IE");
		countries.addItem("Islândia", "IS");
		countries.addItem("Israel", "IL");
		countries.addItem("Itália", "IT");
		countries.addItem("Jamaica", "JM");
		countries.addItem("Japão", "JP");
		countries.addItem("Jersey", "JE");
		countries.addItem("Jordânia", "JO");
		countries.addItem("Kosovo", "XK");
		countries.addItem("Kuwait", "KW");
		countries.addItem("Lesoto", "LS");
		countries.addItem("Letônia", "LV");
		countries.addItem("Líbano", "LB");
		countries.addItem("Libéria", "LR");
		countries.addItem("Líbia Árabe Jamahiriya", "LY");
		countries.addItem("Lichtenstein", "LI");
		countries.addItem("Lituânia", "LT");
		countries.addItem("Luxemburgo", "LU");
		countries.addItem("Macedônia", "MK");
		countries.addItem("Madagascar", "MG");
		countries.addItem("Malásia", "MY");
		countries.addItem("Malauí", "MW");
		countries.addItem("Maldivas", "MV");
		countries.addItem("Mali", "ML");
		countries.addItem("Malta", "MT");
		countries.addItem("Marrocos", "MA");
		countries.addItem("Martinica", "MQ");
		countries.addItem("Maurício", "MU");
		countries.addItem("Mauritânia", "MR");
		countries.addItem("Mayotte", "YT");
		countries.addItem("México", "MX");
		countries.addItem("Mianmar [Birmânia]", "MM");
		countries.addItem("Micronésia", "FM");
		countries.addItem("Moldova, República de", "MD");
		countries.addItem("Mônaco", "MC");
		countries.addItem("Mongólia", "MN");
		countries.addItem("Montenegro", "ME");
		countries.addItem("Montserrat", "MS");
		countries.addItem("Namíbia", "NA");
		countries.addItem("Nauru", "NR");
		countries.addItem("Nepal", "NP");
		countries.addItem("Nicarágua", "NI");
		countries.addItem("Níger", "NE");
		countries.addItem("Nigéria", "NG");
		countries.addItem("Niue", "NU");
		countries.addItem("Noruega", "NO");
		countries.addItem("Nova Caledônia", "NC");
		countries.addItem("Nova Zelândia", "NZ");
		countries.addItem("Omã", "OM");
		countries.addItem("Países Baixos", "NL");
		countries.addItem("Palau", "PW");
		countries.addItem("Panamá", "PA");
		countries.addItem("Papua-Nova Guiné", "PG");
		countries.addItem("Paquistão", "PK");
		countries.addItem("Paraguai", "PY");
		countries.addItem("Peru", "PE");
		countries.addItem("Pitcairn", "PN");
		countries.addItem("Polinésia Francesa", "PF");
		countries.addItem("Polônia", "PL");
		countries.addItem("Porto Rico", "PR");
		countries.addItem("Portugal", "PT");
		countries.addItem("Quênia", "KE");
		countries.addItem("Quirguistão", "KG");
		countries.addItem("Quiribati", "KI");
		countries.addItem("Região Administrativa Especial de Macau", "MO");
		countries.addItem("Reino Unido", "GB");
		countries.addItem("República Centro-Africana", "CF");
		countries.addItem("República Checa", "CZ");
		countries.addItem("República da Guiné-Bissau", "GW");
		countries.addItem("República de Moçambique", "MZ");
		countries.addItem("República Democrática de São Tomé e Príncipe", "ST");
		countries.addItem("República Democrática de Timor-Leste", "TL");
		countries.addItem("República Dominicana", "DO");
		countries.addItem("República Popular Democrática do Laos", "LA");
		countries.addItem("Reunião", "RE");
		countries.addItem("Romênia", "RO");
		countries.addItem("Ruanda", "RW");
		countries.addItem("Rússia", "RU");
		countries.addItem("Saara Ocidental", "EH");
		countries.addItem("Saint Pierre e Miquelon", "PM");
		countries.addItem("Samoa Americana", "AS");
		countries.addItem("Samoa", "WS");
		countries.addItem("San Marino", "SM");
		countries.addItem("Santa Helena", "SH");
		countries.addItem("Santa Lúcia", "LC");
		countries.addItem("Santa Sé (Cidade-Estado do Vaticano)", "VA");
		countries.addItem("São Bartolomeu", "BL");
		countries.addItem("São Cristóvão e Névis", "KN");
		countries.addItem("São Martinho", "MF");
		countries.addItem("São Martinho", "SX");
		countries.addItem("São Vincente e Granadinas", "VC");
		countries.addItem("Senegal", "SN");
		countries.addItem("Serra Leoa", "SL");
		countries.addItem("Sérvia", "RS");
		countries.addItem("Síria", "SY");
		countries.addItem("Somália", "SO");
		countries.addItem("Sri Lanka", "LK");
		countries.addItem("Suazilândia", "SZ");
		countries.addItem("Sudão do Sul", "SS");
		countries.addItem("Sudão", "SD");
		countries.addItem("Suécia", "SE");
		countries.addItem("Suíça", "CH");
		countries.addItem("Suriname", "SR");
		countries.addItem("Svalbard e Jan Mayen", "SJ");
		countries.addItem("Tailândia", "TH");
		countries.addItem("Taiwan", "TW");
		countries.addItem("Tajiquistão", "TJ");
		countries.addItem("Tanzânia", "TZ");
		countries.addItem("Território Britânico do Oceano Índico", "IO");
		countries.addItem("Territórios Franceses do Sul", "TF");
		countries.addItem("Territórios palestinos", "PS");
		countries.addItem("Togo", "TG");
		countries.addItem("Tokelau", "TK");
		countries.addItem("Tonga", "TO");
		countries.addItem("Trindade e Tobago", "TT");
		countries.addItem("Tunísia", "TN");
		countries.addItem("Turcomenistão", "TM");
		countries.addItem("Turquia", "TR");
		countries.addItem("Tuvalu", "TV");
		countries.addItem("Ucrânia", "UA");
		countries.addItem("Uganda", "UG");
		countries.addItem("Uruguai", "UY");
		countries.addItem("Uzbequistão", "UZ");
		countries.addItem("Vanuatu", "VU");
		countries.addItem("Venezuela", "VE");
		countries.addItem("Vietnã", "VN");
		countries.addItem("Zâmbia", "ZM");
		countries.addItem("Zimbábue", "ZW");

		return countries;
	}
	
	public ListBox getBrazilianStatesList(){
		ListBox states = new ListBox();

		states.addItem("Selecione:", "-");
		states.addItem("Acre");
		states.addItem("Alagoas");
		states.addItem("Amapá");
		states.addItem("Amazonas");
		states.addItem("Bahia");
		states.addItem("Ceará");
		states.addItem("Distrito Federal");
		states.addItem("Espírito Santo");
		states.addItem("Goiás");
		states.addItem("Maranhão");
		states.addItem("Mato Grosso");
		states.addItem("Mato Grosso do Sul");
		states.addItem("Minas Gerais");
		states.addItem("Pará ");
		states.addItem("Paraíba");
		states.addItem("Paraná");
		states.addItem("Pernambuco");
		states.addItem("Piauí");
		states.addItem("Rio de Janeiro");
		states.addItem("Rio Grande do Norte");
		states.addItem("Rio Grande do Sul");
		states.addItem("Rondônia");
		states.addItem("Roraima");
		states.addItem("Santa Catarina");
		states.addItem("São Paulo");
		states.addItem("Sergipe");
		states.addItem("Tocantins");

		return states;
	}
	
	public ListBox getSexList(){
		ListBox sex = new ListBox();
		
		sex.addItem("Selecione:", "-");
		sex.addItem("Feminino", "F");
		sex.addItem("Masculino", "M");

		return sex;
	}

	public boolean isListBoxSelected(ListBox value) {
		return value != null && !"-".equals(((ListBox)value).getValue());
	}

	public Date getDateFromString(String dateStr) {
		if(dateStr == null)
			return null;
		DateTimeFormat dtf = new DateTimeFormat("yyyy-MM-dd", new DefaultDateTimeFormatInfo()) {};  // <= trick here
		Date date = dtf.parse(dateStr);
		return date;
	}
	
	public TextBoxFormField createTextBoxFormField(String text){
		TextBox fieldTextBox = new TextBox();
		fieldTextBox.addStyleName("field");
		fieldTextBox.addStyleName("textField");
		fieldTextBox.setValue(text);
		return new TextBoxFormField(fieldTextBox);
	}

	public void clearErrors(List<KornellFormFieldWrapper> fields) {
		for (KornellFormFieldWrapper field : fields) {
			field.clearError();
		}
	}
		
}