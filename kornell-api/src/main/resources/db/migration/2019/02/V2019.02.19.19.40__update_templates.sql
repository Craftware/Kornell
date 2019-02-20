UPDATE EmailTemplate set title = '$$COURSE_NAME$$ disponível em $$INSTITUTION_NAME$$',
template = '<div style="width: 700px;margin: 0 auto;padding: 20px 50px;border: 1px solid #CACACA;color: #444444;font-size: 18px;font-family: Helvetica,Arial,sans-serif;-webkit-box-shadow: 14px 14px 5px #AFAFAF;-moz-box-shadow: 14px 14px 5px #AFAFAF;box-shadow: 14px 14px 5px #AFAFAF;margin-bottom: 30px;border-radius: 10px;">
<p>Ol&aacute;, <b>$$PERSON_FULLNAME$$</b></p>
<p>&nbsp;</p>
<p>O conte&uacute;do <b>$$CLASS_NAME$$</b> &centerdot; <b>$$COURSE_NAME$$</b> oferecido por <b>$$INSTITUTION_NAME$$</b> est&aacute; dispon&iacute;vel para voc&ecirc;.</p>
<p>Utilize o bot&atilde;o abaixo para acessar.</p>
<div style="width: 300px;margin: 0 auto;margin-bottom: 50px;margin-top: 50px;text-align: center;display: block;height: auto;">
	<a href="$$BUTTON_LINK$$" target="_blank" style="text-decoration: none;">
		<div style="display: block;width: 220px;height: 30px;line-height: 28px;margin-right: 30px;text-shadow: 1px 1px 1px rgba(0, 0, 0, 0.5);font-size: 14px;font-weight: bold;color: #e9e9e9;border-radius: 5px;display: block;border: 0px;background: #4c4f54;background: -moz-linear-gradient(top, #4c4f54 0%, #252629 100%);background: -webkit-gradient(linear, left top, left bottom, color-stop(0%, #4c4f54), color-stop(100%, #252629));background: -webkit-linear-gradient(top, #4c4f54 0%, #252629 100%);background: -o-linear-gradient(top, #4c4f54 0%, #252629 100%);background: -ms-linear-gradient(top, #4c4f54 0%, #252629 100%);background: linear-gradient(to bottom, #4c4f54 0%, #252629 100%);filter: progid:DXImageTransform.Microsoft.gradient( startColorstr=''#4c4f54'', endColorstr=''#252629'',GradientType=0 );margin: 0 auto;">Acessar</div>
	</a>
</div>
<p>Caso seja seu primeiro acesso, cadastre-se inicialmente na plataforma utilizando este email: <b>$$PERSON_EMAIL$$</b></p>
<p>&nbsp;</p>
<p>Cordialmente,</p>
<p><b>$$INSTITUTION_NAME$$</b></p>
<img alt="" src="cid:logo" style="width: 300px;height: 80px;margin: 0 auto;display: block;"></div>'
where templateType = 'ENROLLMENT_CONFIRMATION' and locale = 'pt_BR';


UPDATE EmailTemplate set title = 'Uma nova conversa de ajuda criada para: $$CLASS_NAME$$ · $$COURSE_NAME$$',
template = '<div style="width: 700px;margin: 0 auto;padding: 20px 50px;border: 1px solid #CACACA;color: #444444;font-size: 18px;font-family: Helvetica,Arial,sans-serif;-webkit-box-shadow: 14px 14px 5px #AFAFAF;-moz-box-shadow: 14px 14px 5px #AFAFAF;box-shadow: 14px 14px 5px #AFAFAF;margin-bottom: 30px;border-radius: 10px;">
<p>Ol&aacute;, <b>$$PERSON_FULLNAME$$</b></p>
<p>&nbsp;</p>
<p><b>$$PARTICIPANT_FULLNAME$$</b> <b>($$PARTICIPANT_EMAIL$$)</b> criou uma nova conversa de ajuda sobre: <b>$$CLASS_NAME$$</b> &centerdot; <b>$$COURSE_NAME$$</b>.</p>
<p>&nbsp;</p>
<p>Mensagem: <br /><br /><i>$$THREAD_MESSAGE$$</i></p>
<div style="width: 300px;margin: 0 auto;margin-bottom: 50px;margin-top: 50px;text-align: center;display: block;height: auto;">
	<a href="$$BUTTON_LINK$$" target="_blank" style="text-decoration: none;">
		<div style="display: block;width: 220px;height: 30px;line-height: 28px;margin-right: 30px;text-shadow: 1px 1px 1px rgba(0, 0, 0, 0.5);font-size: 14px;font-weight: bold;color: #e9e9e9;border-radius: 5px;display: block;border: 0px;background: #4c4f54;background: -moz-linear-gradient(top, #4c4f54 0%, #252629 100%);background: -webkit-gradient(linear, left top, left bottom, color-stop(0%, #4c4f54), color-stop(100%, #252629));background: -webkit-linear-gradient(top, #4c4f54 0%, #252629 100%);background: -o-linear-gradient(top, #4c4f54 0%, #252629 100%);background: -ms-linear-gradient(top, #4c4f54 0%, #252629 100%);background: linear-gradient(to bottom, #4c4f54 0%, #252629 100%);filter: progid:DXImageTransform.Microsoft.gradient( startColorstr=''#4c4f54'', endColorstr=''#252629'',GradientType=0 );margin: 0 auto;">Acessar</div>
	</a>
</div>
<p>Cordialmente,</p>
<p><b>$$INSTITUTION_NAME$$</b></p>
<img alt="" src="cid:logo" style="width: 300px;height: 80px;margin: 0 auto;display: block;"></div>'
where templateType in  ('NEW_SUPPORT_CHAT_THREAD', 'NEW_TUTORING_CHAT_THREAD') and locale = 'pt_BR';

UPDATE EmailTemplate set title = 'Uma nova conversa de ajuda criada para: $$INSTITUTION_SHORTNAME$$', template = '<div style="width: 700px;margin: 0 auto;padding: 20px 50px;border: 1px solid #CACACA;color: #444444;font-size: 18px;font-family: Helvetica,Arial,sans-serif;-webkit-box-shadow: 14px 14px 5px #AFAFAF;-moz-box-shadow: 14px 14px 5px #AFAFAF;box-shadow: 14px 14px 5px #AFAFAF;margin-bottom: 30px;border-radius: 10px;">
<p>Ol&aacute;, <b>$$PERSON_FULLNAME$$</b></p>
<p>&nbsp;</p>
<p><b>$$PARTICIPANT_FULLNAME$$</b> <b>($$PARTICIPANT_EMAIL$$)</b> criou uma nova conversa de ajuda.</p>
<p>&nbsp;</p>
<p>Mensagem: <br /><br /><i>$$THREAD_MESSAGE$$</i></p>
<div style="width: 300px;margin: 0 auto;margin-bottom: 50px;margin-top: 50px;text-align: center;display: block;height: auto;">
	<a href="$$BUTTON_LINK$$" target="_blank" style="text-decoration: none;">
		<div style="display: block;width: 220px;height: 30px;line-height: 28px;margin-right: 30px;text-shadow: 1px 1px 1px rgba(0, 0, 0, 0.5);font-size: 14px;font-weight: bold;color: #e9e9e9;border-radius: 5px;display: block;border: 0px;background: #4c4f54;background: -moz-linear-gradient(top, #4c4f54 0%, #252629 100%);background: -webkit-gradient(linear, left top, left bottom, color-stop(0%, #4c4f54), color-stop(100%, #252629));background: -webkit-linear-gradient(top, #4c4f54 0%, #252629 100%);background: -o-linear-gradient(top, #4c4f54 0%, #252629 100%);background: -ms-linear-gradient(top, #4c4f54 0%, #252629 100%);background: linear-gradient(to bottom, #4c4f54 0%, #252629 100%);filter: progid:DXImageTransform.Microsoft.gradient( startColorstr=''#4c4f54'', endColorstr=''#252629'',GradientType=0 );margin: 0 auto;">Acessar</div>
	</a>
</div>
<p>Cordialmente,</p>
<p><b>$$INSTITUTION_NAME$$</b></p>
<img alt="" src="cid:logo" style="width: 300px;height: 80px;margin: 0 auto;display: block;"></div>'
where templateType in ('NEW_PLATFORM_SUPPORT_CHAT_THREAD', 'NEW_INSTITUTION_SUPPORT_CHAT_THREAD') and locale = 'pt_BR';