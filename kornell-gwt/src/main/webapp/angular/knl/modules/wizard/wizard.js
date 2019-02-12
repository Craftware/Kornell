'use strict';

var app = angular.module('knl');

app.controller('WizardController', [
  '$scope',
  '$timeout',
  '$window',
  '$sce',
  '$location',
  '$uibModal',
  'FileUploader',
  'LECTURE_TYPES',
  function($scope, $timeout, $window, $sce, $location, $uibModal, FileUploader, LECTURE_TYPES) {

    $scope.postMessageToParentFrame("wizardReady", "");

    window.addEventListener('message',function(event) {
      if(event.data.type === 'classroomJsonLoad'){
        $scope.root = JSON.parse(event.data.message);
        $scope.initWizard();
      } else if(event.data.type === 'classroomJsonNew') {
        $scope.newTree(event.data.message);
        $scope.initWizard();
      } else if(event.data.type === 'classroomJsonSaved'){
        $scope.blockPublishButton = false;
      } else if(event.data.type === 'lastClassroomJsonPublished'){
        $scope.lastClassroomJsonPublished = JSON.parse(event.data.message);
        $scope.verifyTree();
      }
    },false);

    $scope.isUnsavedNodes = {};
    $scope.isUnpublishedNodes = {};
    
    $scope.initWizard = function(){

      console.log($scope.root);

      $scope.uiTreeOptions = {
        accept: function (src, dest, destIndex) {
          switch(dest.depth()){
            case 1:
              return (src.$modelValue.itemType === 'module');
            case 2:
              return (src.$modelValue.itemType === 'lecture');
            default:
              return false;
          }
        },
        dropped: function (event) {
        }
      };

      $scope.colorPickeroptions = {
        format: 'hex', 
        alpha: false
      };

      angular.element($window).bind('resize', function(){
        $scope.calculateLimitToDots();
        $scope.$digest();
      });

      $scope.questionPickerSortableOptions = {
        stop: function(e, ui) { 
          var item = ui.item[0],
            newPosition = parseInt(ui.item[0].innerText - 1);
          if(newPosition <= $scope.selectedQuestionIndex && $scope.oldPosition > $scope.selectedQuestionIndex){
            $scope.selectQuestion($scope.selectedQuestionIndex + 1);
          } else if(newPosition >= $scope.selectedQuestionIndex && $scope.oldPosition < $scope.selectedQuestionIndex){
            $scope.selectQuestion($scope.selectedQuestionIndex - 1);
          } else if($scope.oldPosition === $scope.selectedQuestionIndex){
            $scope.selectQuestion(newPosition);
          }
        },
        update: function(e, ui) {
          $scope.oldPosition = parseInt(ui.item[0].innerText - 1);
        }
      };
      
      $scope.blankQuestion = {
        text: 'At vero eos et accusamus et iusto odio dignissimos ducimus qui blanditiis praesentium?',
        options: [
          {
            text: "Voluptatum deleniti atque corrupti quos dolores et quas molestias excepturi sint occaecati",
            expected: true
          },
          {
            text: "Cupiditate non provident, similique sunt in culpa qui officia deserunt mollitia animi",
            expected: false
          },
          {
            text: "Et harum quidem rerum facilis est et expedita distinctio",
            expected: false
          },
          {
            text: "Nam libero tempore, cum soluta nobis est eligendi optio cumque nihil",
            expected: false
          }
        ]
      };

      $scope.blankFaq = {
          question: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat?',
          answer: 'Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.'
      };
      
      $scope.data = [$scope.root];

      $scope.calculateLimitToDots();

      $scope.verifyTree();
      $scope.lastClassroomJsonSaved = angular.copy($scope.root);

      $scope.$watch('root', function() {
        $scope.verifyTreeHelper();
      }, true);

      $timeout(function(){
        if(localStorage['KNLw.hideHelpOnStart'] !== "true"){
          $scope.goToNode($scope.root.uuid);
          $timeout(function(){
            $scope.startTreeHelp();
            localStorage['KNLw.hideHelpOnStart'] = true;
          });
        } else {
          var lastExists = $scope.getNodeByUUID(localStorage['KNLw.lastVisitedLectureUUID']);
          $scope.goToNode((lastExists && localStorage['KNLw.lastVisitedLectureUUID']) || $scope.lastLectureUUID || $scope.lastModuleUUID);
        }
      });
      
    };

    $scope.toggleBlockAdvance = function() {
      if($scope.selectedNode.blockAdvance){
        $scope.selectedNode.blockAdvanceDate = moment.now();
      } else {
        $scope.selectedNode.blockAdvanceDate = null;
      }
    };

    $scope.treeHelpOptions = {
        steps: [
            {
                element: '#treeHelp1',
                intro: 'Aqui você monta a estrutura do seu curso, agrupando as aulas dentro de módulos. Cada aula é uma página. Existem vários tipos de aula, como imagem, texto, vídeo, youtube, exercício, etc.',
                position: 'right'
            },
            {
                element: '#treeHelp2',
                intro: 'O conteúdo é salvo automaticamente à medida que o edita. Mas o fato de ser salvo não quer dizer que os participantes matriculados nas turmas desse curso terão acesso a esse conteúdo. Para que isso aconteça, é necessário que você publique o conteúdo, disponibilizando-o para todos os alunos.',
                position: 'right'
            },
            {
                element: '#treeHelp3',
                intro: 'A qualquer momento você pode reverter todas as alterações feitas desde a última publicação.',
                position: 'right'
            },
            {
                element: '#treeHelp4',
                intro: 'Use esse botão para adicionar novos módulos.',
                position: 'right'
            },
            {
                element: '#treeHelp5',
                intro: 'E dentro de cada módulo você pode usar esse botão para adicionar novas aulas.',
                position: 'right'
            },
            {
                element: '#treeHelp6',
                intro: 'Você também pode reorganizar a ordem das suas aulas ou módulos usando esse botão. Simplesmente arraste e solte o item para o novo local desejado.',
                position: 'right'
            },
            {
                element: '#treeHelp7',
                intro: 'Para cada versão criada, existe uma turma "sandbox", onde os administradores da instituição e publicadores de conteúdo podem testar as alterações no conteúdo de seus cursos. A qualquer momento, um administrador pode reiniciar todas as matrículas da turma, caso seja necessário testar alguma alteração. Lembre-se que em turmas "sandbox" você verá o conteúdo que está SALVO, ao invés de ver o conteúdo publicado.',
                position: 'right'
            },
            {
                element: '#none',
                intro: 'Se tiver qualquer outra dúvida ou sugestão, entre em contato com o suporte clicando no botão de ajuda no canto superior direito da plataforma.',
                position: 'right'
            }
        ],
        showStepNumbers: false,
        showBullets: true,
        showProgress: true,
        exitOnOverlayClick: (localStorage['KNLw.hideHelpOnStart'] === "true"),
        exitOnEsc: (localStorage['KNLw.hideHelpOnStart'] === "true"),
        nextLabel: 'Próxima',
        prevLabel: 'Anterior',
        skipLabel: 'Fechar',
        doneLabel: 'Fechar'
    };

    $scope.goToSandboxClassroom = function(){
      $scope.postMessageToParentFrame("goToSandboxClassroom", "");
    };

    $scope.publishTree = function(){
      if($scope.blockPublishButton) {
        return;
      }
      var publishTreeMessage = "Suas alterações até o momento foram salvas automaticamente, mas ainda não foram publicadas.<br><br>" +
        "Tem certeza que deseja publicá-las? O conteúdo será imediatamente acessível para todos os participantes de turmas que utilizam essa versão. Essa operação não poderá ser desfeita.";
      var publishTreeCallback = function(){
        $scope.blockPublishButton = true;
        $scope.root.publishingUUID = $scope.uuid();
        $scope.lastClassroomJsonSaved = angular.copy($scope.root);
        $scope.verifyTree();
        var contents = decodeURI(Base64.decode(localStorage.KNLwp));
        $scope.postMessageToParentFrame("wizardPublish", contents);
      };
      $scope.confirmModal(publishTreeMessage, publishTreeCallback);
    };

    $scope.discardTree = function() {
      //TODO
      if($scope.blockPublishButton) {
        return;
      }
      var discardTreeMessage = "Tem certeza que deseja descartar TODAS as alterações desde a última publicação? Essa operação não poderá ser desfeita.";
      var discardTreeCallback = function(){
        $scope.blockPublishButton = true;
        $scope.root = angular.copy($scope.lastClassroomJsonSaved);
        $scope.data = [$scope.root];
        $scope.goToNode($scope.selectedNode.uuid);
        $scope.postMessageToParentFrame("wizardDiscard", "");
      };
      $scope.confirmModal(discardTreeMessage, discardTreeCallback);
    };

    $scope.confirmModal = function(message, callback) {
      var modalInstance = $uibModal.open({
        animation: true,
        component: 'confirmModal',
        resolve: {
          message: function() {
            return message;
          }
        }
      });

      modalInstance.result.then(
        function (selectedType) {
          callback();
        },
        function(){
        }
      );
    };

    $scope.saveTree = function() {
      $scope.blockPublishButton = true;
      $scope.lastClassroomJsonSaved = angular.copy($scope.root);
      $scope.verifyTree();
      var contents = angular.toJson(angular.fromJson(decodeURI(Base64.decode(localStorage.KNLwp))));
      $scope.postMessageToParentFrame("wizardSave", contents);
    };

    $scope.verifyTreeHelper = function() {
      if($scope.verifyTimer){
        $timeout.cancel($scope.verifyTimer);
      }
      $scope.verifyTimer = $timeout(function(){
        $scope.verifyTree();
        if($scope.isUnsaved) {
          $scope.saveTree();
        }
      }, 1000);
    };

    $scope.verifyTree = function() {

        var verifySavedStatuses = function(node) {
          verifySavedStatus(node, 'lastClassroomJsonSaved');
          verifySavedStatus(node, 'lastClassroomJsonPublished');
        };

        var verifySavedStatus = function(node, source) {
          var concatenateUUIDs = function(array) {
            var uuids = "";
            angular.forEach(array, function(obj){
              uuids += obj.uuid + ",";
            });
            return uuids;
          };

          source = source || 'lastClassroomJsonSaved';
          var statusAttribute = (source == 'lastClassroomJsonSaved') ? 'isUnsaved' : 'isUnpublished';
          if($scope[source]) {
            var o1 = angular.copy(node),
                o2 = angular.copy($scope.getNodeByUUID(node.uuid, source));
            if(o2){
              //$scope[statusAttribute+'Nodes'][o1.uuid] = false;
              //$scope[statusAttribute+'Nodes'][o2.uuid] = false;
              if(o1.itemType === 'root'){
                o1.modules = concatenateUUIDs(o1.modules);
                o2.modules = concatenateUUIDs(o2.modules);
              } else if(o1.itemType === 'module'){
                o1.lectures = concatenateUUIDs(o1.lectures);
                o2.lectures = concatenateUUIDs(o2.lectures);
                o1.count = o2.count = 0;
              } else if(o1.itemType === 'lecture'){
                o1.count = o2.count = o1.totalLecturesCount = o2.totalLecturesCount = 0;
              }
            }
            if(!angular.equals(o1, o2)){
              $scope[statusAttribute+'Nodes'][node.uuid] = true;
              $scope[statusAttribute] = true;
            } else {
              $scope[statusAttribute+'Nodes'][node.uuid] = false;
            }
          }
        };

        if(!$scope.root) return;
        $scope.root.uuid = $scope.root.uuid || $scope.uuid();
        $scope.root.files = $scope.root.files || {};
        $scope.isUnsaved = false;
        $scope.isUnpublished = false;
        $scope.hasFinalExamLecture = false;
        $scope.lectureUUIDs = [];
        var modulesCount = 0, totalLecturesCount = 0;
        angular.forEach($scope.root.modules, function(module){
          $scope.lastModuleUUID = module.uuid;
          module.uuid = module.uuid || $scope.uuid();
          module.itemType = "module";
          module.parentUUID = $scope.root.uuid;
          module.count = modulesCount++;
          var lecturesCount = 0;
          angular.forEach(module.lectures, function(lecture){
            $scope.lastLectureUUID = lecture.uuid;
            lecture.uuid = lecture.uuid || $scope.uuid();
            lecture.itemType = "lecture";
            lecture.parentUUID = module.uuid;
            lecture.count = lecturesCount++;
            lecture.totalLecturesCount = totalLecturesCount++;
            if(lecture.type === 'bubble'){
              $scope.lastBubbleColor2 = (lecture.color2 && lecture.color2.length) ? lecture.color2 : $scope.lastBubbleColor2;
            }
            if(lecture.type === 'finalExam'){
              $scope.hasFinalExamLecture = true;
              angular.forEach(lecture.questions, function(question){
                question.uuid = question.uuid || $scope.uuid();
                angular.forEach(question.options, function(option){
                  option.uuid = option.uuid || $scope.uuid();
                });
              });
            }
            if(lecture.type === 'faq'){
              angular.forEach(lecture.faqs, function(faq){
                faq.uuid = faq.uuid || $scope.uuid();
              });
            }
            if(lecture.blockAdvanceDate){
              lecture.blockAdvanceDate = lecture.blockAdvanceDate.valueOf ? lecture.blockAdvanceDate.valueOf() : lecture.blockAdvanceDate;
            }
            $scope.lectureUUIDs.push(lecture.uuid);
            verifySavedStatuses(lecture);
          });
          verifySavedStatuses(module);
        });
        angular.forEach($scope.root.files, function(file, attributeName){
          if(attributeName != '_baseURL'){
            if(!file.uploadedURL && !file.hostedURL && file.type == 'uploaded'){
              delete $scope.root.files[attributeName];
            }
          }
        });
        verifySavedStatuses($scope.root);
        localStorage.KNLwp = Base64.encode(encodeURI(angular.toJson($scope.root)));
    };

    $scope.getNodeByUUID = function(uuid, source){
      var found;
      var tree = source ? $scope[source] : $scope.root;
      if(tree.uuid === uuid) found = tree;
      angular.forEach(tree.modules, function(module){
        if(module.uuid === uuid) found = module;
        angular.forEach(module.lectures, function(lecture){
          if(lecture.uuid === uuid) found = lecture;      
        });
      });
      return found;
    };

    $scope.previousLecture = function(){
      for(var i = 0; i < $scope.lectureUUIDs.length; i++){
        if($scope.lectureUUIDs[i] === $scope.selectedNode.uuid){
          if((i - 1) >= 0){
            $scope.keepViewTab = true;
            $scope.goToNode($scope.lectureUUIDs[i-1]);
          }
        }
      }
    };

    $scope.nextLecture = function(){
      for(var i = 0; i < $scope.lectureUUIDs.length; i++){
        if($scope.lectureUUIDs[i] === $scope.selectedNode.uuid){
          if((i + 1) < $scope.lectureUUIDs.length){
            $scope.keepViewTab = true;
            $scope.goToNode($scope.lectureUUIDs[i+1]);
          }
        }
      }
    };

    $scope.getLectureNameByType = function(type){
      for(var i = 0; i < LECTURE_TYPES.length; i++){
        if(LECTURE_TYPES[i].type == type){
          return LECTURE_TYPES[i].name;
        }
      }
    };

    $scope.newLecture = function(scope){
      var nodeData = scope.$modelValue;
      var modalInstance = $uibModal.open({
        animation: true,
        component: 'addLectureModal',
        resolve: {
          hasFinalExamLecture: function() {
            return $scope.hasFinalExamLecture
          }
        }
      });

      modalInstance.result.then(
        function (selectedType) {
          var lectureName = 'Aula ' + (nodeData.count + 1) + '.' + (nodeData.lectures.length + 1) + ': ' + $scope.getLectureNameByType(selectedType);
          var lecture = {
            itemType: 'lecture',
            title: lectureName,
            type: selectedType,
            uuid: $scope.uuid()
          };
          if(lecture.type == 'bubble') {
            lecture.text = 'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.';
            lecture.id = 'https://images.pexels.com/photos/428341/pexels-photo-428341.jpeg?w=200&h=200&auto=compress';
            lecture.text2 = 'Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.';
            lecture.id2 = 'https://images.pexels.com/photos/415829/pexels-photo-415829.jpeg?w=200&h=200&auto=compress';
            lecture.color2 = $scope.lastBubbleColor2 || '3D3D40';
          } else if(lecture.type == 'youtube') {
            lecture.id = 's1mczCFXcSY';
          } else if(lecture.type == 'vimeo') {
            lecture.id = '242792908';
          } else if(lecture.type == 'video') {

          } else if(lecture.type == 'question') {
            lecture.text = $scope.blankQuestion.text;
            lecture.isMultiple = false;
            lecture.shuffleQuestions = false;
            lecture.options = angular.copy($scope.blankQuestion.options);
          } else if(lecture.type == 'finalExam') {
            lecture.questions = [angular.copy($scope.blankQuestion), angular.copy($scope.blankQuestion), angular.copy($scope.blankQuestion)];
          } else if(lecture.type == 'text') {
            lecture.text = 'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.';
            lecture.showWell = true;
          } else if(lecture.type == 'image') {
            lecture.id = 'https://static.pexels.com/photos/355988/pexels-photo-355988.jpeg';
          }  else if(lecture.type == 'faq') {
            lecture.faqs = [$scope.blankFaq];
          }
          nodeData.lectures.push(lecture);
          $scope.goToNode(lecture.uuid);
        },
        function(){

        }
      );
    };

    $scope.newQuestion = function(selectedQuestion) {
      $scope.selectedNode.questions.push(angular.copy($scope.blankQuestion));
      $scope.selectQuestion($scope.selectedNode.questions.length - 1);
    };

    $scope.removeQuestion = function(index) {
      $scope.selectedNode.questions.splice(index, 1);
      $scope.selectQuestion(0);
      $scope.limitQuestionsChanged();
    };

    $scope.newOption = function(selectedQuestion) {
      selectedQuestion.options.push({text: 'Quibusdam et aut officiis debitis aut rerum necessitatibus saepe eveniet'});
    };

    $scope.newTree = function(courseTitle){
      $scope.root = {
        title: courseTitle,
        availableVideoSizes: "144,360,720",
        colorBackground: "EBEBEB",
        colorFont: "0284B5",
        colorTheme: "0284B5",
        colorTitle: "EBEBEB",
        itemType: "root",
        paddingTopIframe: 56,
        modules: [],
        uuid: $scope.uuid()
      };
      $scope.newModule();
    }

    $scope.newModule = function(goToModule){
      var newModule = {
        itemType: 'module',
        title: 'Módulo ' + ($scope.root.modules.length + 1),
        uuid: $scope.uuid(),
        lectures: []
      };
      $scope.root.modules.push(newModule);
      if(goToModule){
        $scope.goToNode(newModule.uuid);
      }
    };

    $scope.refreshSlimScroll = function () {
      $timeout(function(){
        $('#editPanelScroll').slimScroll({
            alwaysVisible: true
        });
        $('widthUnder991Scroll').slimScroll({
            alwaysVisible: true
        });
      });
    };

    $scope.edit = function (scope) {
      $scope.selectedNode = scope.$modelValue;
      $scope.selectedNodeSaved = $scope.getNodeByUUID($scope.selectedNode.uuid, 'lastClassroomJsonSaved');
      $scope.selectedNodeScope = scope;
      if(!$scope.keepViewTab){
        $scope.selectedTab = 'edit';
      }
      $scope.keepViewTab = false;
      $scope.previewURL = $sce.trustAsResourceUrl(
        "knlClassroom/index.html?cache-buster="+(new Date().getTime())+"#!/lecture" + 
        "?preview=1" + 
        "&uuid=" + $scope.selectedNode.uuid);
      var editPanel = $('#editPanel').get(0)
      editPanel && editPanel.scrollIntoView();
      $scope.refreshSlimScroll();
      localStorage['KNLw.lastVisitedLectureUUID'] = scope.selectedNode.uuid;
    };

    $scope.remove = function () {
      if($scope.selectedNodeScope){

        //cleanup the files associated with the node
        angular.forEach($scope.root.files, function(file, attributeName){
          if(attributeName.indexOf($scope.selectedNode.uuid) == 0){
            delete $scope.root.files[attributeName];
          }
        });

        var node = $scope.selectedNode,
            parentUUID = node.parentUUID,
            parentArrayAttribute = node.itemType + 's', //modules or lectures
            parentArray = $scope.getNodeByUUID(parentUUID)[parentArrayAttribute];
        if(node.count == 0 && parentArray.length == 1){
          // go to parent if it's the last
          $scope.goToNode(parentUUID);
        } else if(node.count == 0){
          // if it's the first, go to the next
          $scope.goToNode(parentArray[node.count + 1].uuid);
        } else {
          // otherwise go to the previous
          $scope.goToNode(parentArray[node.count - 1].uuid);
        }
        $scope.selectedNodeScope.remove();
      }

    };

    $scope.goToNode = function(uuid){
      $timeout(function(){
        $('#nodeEdit_'+uuid)[0] && $('#nodeEdit_'+uuid)[0].click();
      });
    };

    $scope.isMultipleClicked = function(lecture){
      if(!lecture.isMultiple){
        angular.forEach(lecture.options, function(option, i){
          if(option.expected){
            $scope.optionClicked(lecture, option, i);
          } else if (i === (lecture.options.length - 1)) {
            $scope.optionClicked(lecture, lecture.options[0], 0);
          } else {
            option.expected = false;
          }
        });
      }
    };

    $scope.optionClicked = function(lecture, option, index){
      if(!lecture.isMultiple && option.expected){
        angular.forEach(lecture.options, function(o, i){
          if(i != index){
            o.expected = false;
          }
        });
      } else {
        lecture.options[index].expected = false;
      }
    };

    $scope.selectQuestion = function(index) {
      $scope.selectedQuestionIndex = index;
      $scope.selectedQuestion = $scope.selectedNode.questions[index];
    };

    $scope.expectedGradeChanged = function() {
      $timeout(function(){
        if($scope.selectedNode.expectedGrade === undefined || $scope.selectedNode.expectedGrade < 0 || $scope.selectedNode.expectedGrade > 100) {
          $scope.selectedNode.expectedGrade = null;
        }
      },1000);
    };

    $scope.timeLimitChanged = function() {
      $timeout(function(){
        if(!$scope.selectedNode.timeLimit || $scope.selectedNode.timeLimit <= 0) {
          delete $scope.selectedNode.timeLimit;
        }
      },1000);
    };

    $scope.limitQuestionsChanged = function() {
      $timeout(function(){
        if(!$scope.selectedNode.shuffleQuestions){
          $scope.selectedNode.limitQuestions = null;
        } else if($scope.selectedNode.limitQuestions === undefined || $scope.selectedNode.limitQuestions < 1 || $scope.selectedNode.limitQuestions > $scope.selectedNode.questions.length) {
          $scope.selectedNode.limitQuestions = $scope.selectedNode.questions.length;
        }
      },1000);
    };

    $scope.toggleOptions = function() {
      angular.forEach($scope.selectedNode.questions, function(question){
        $scope.toggleOption(question);
      });
    };

    $scope.toggleOption = function(question, index){
      var selectedIndexes = [];
      angular.forEach(question.options, function(o, i){
        if(o.expected) {
          selectedIndexes.push(i);
        }
      });
      // select multiple was toggled
      if(index === undefined && !$scope.selectedNode.isMultiple){
        // toggle first option if none was clicked
        if(!selectedIndexes.length){
          question.options[0].expected = true;
        } else {
          // if there's more than one selected, keep only one if it's not multiple choice      
          angular.forEach(question.options, function(o, i){
            question.options[i].expected = (i === selectedIndexes[0]);
          });
        }
      } else {
        if(!$scope.selectedNode.isMultiple && question.options[index].expected){
          // if it's not multiple, only the clicked option should be selected
          angular.forEach(question.options, function(o, i){
              o.expected = (i === index);
          });
        } else if(!$scope.selectedNode.isMultiple){
          // if it's not multiple, make sure that one is always selected
          question.options[index].expected = true;
        }
      }
    };

    $scope.trimVimeoURL = function(){
      if($scope.selectedNode.id.indexOf('vimeo.com/') >= 0){
        $scope.sendKornellNotification("success", "O ID do vídeo foi extraído da URL com sucesso");
        $scope.selectedNode.id = $scope.selectedNode.id.split('vimeo.com/')[1];
      }
    };

    $scope.trimYouTubeURL = function(){
      var stripIdFromVideoURL = function(url){
        var r, rx = /^.*(?:(?:youtu\.be\/|v\/|vi\/|u\/\w\/|embed\/)|(?:(?:watch)?\?v(?:i)?=|\&v(?:i)?=))([^#\&\?]*).*/;
        try {
          var url =  url.match(rx)[1];
          $scope.sendKornellNotification("success", "O ID do vídeo foi extraído da URL com sucesso");
          return url;
        } catch(err){
          return url;
        }
      }
      $scope.selectedNode.id = stripIdFromVideoURL($scope.selectedNode.id);
    };

    $scope.calculateLimitToDots = function(){
      if($window.innerWidth > 1800) {
        $scope.limitToDots = 40;
      } else if($window.innerWidth > 1600) {
        $scope.limitToDots = 35;
      } else if($window.innerWidth > 1400) {
        $scope.limitToDots = 30;
      } else if($window.innerWidth > 1200) {
        $scope.limitToDots = 25;
      } else if($window.innerWidth > 1100) {
        $scope.limitToDots = 20;
      } else if($window.innerWidth > 991) {
        $scope.limitToDots = 15;
      } else if($window.innerWidth > 600) {
        $scope.limitToDots = 100;
      } else if($window.innerWidth > 450) {
        $scope.limitToDots = 500;
      } else {
        $scope.limitToDots = 35;
      }
      $scope.innerWidth = $window.innerWidth;
    };

    $scope.initializeJsonDiff = function(){

      if(!$scope.areas){

        var JsonArea = function JsonArea(element) {
          this.element = element;
          this.container = element.parentNode;
          var self = this;
        };

        JsonArea.prototype.parse = function(str) {
          if (/^\d+(.\d+)?(e[\+\-]?\d+)?$/i.test(str) ||
            /^(true|false)$/.test(str) ||
            /^["].*["]$/.test(str) ||
            /^[\{\[](.|\n)*[\}\]]$/.test(str)) {
            return JSON.parse(str, jsondiffpatch.dateReviver);
          }
          return this.editor.getValue();
        };

        JsonArea.prototype.setValue = function(value) {
          if(typeof value === 'string')
            value = this.parse(value)
          this.editor.setValue(JSON.stringify(value, null, 2));
        };

        $scope.areas = {
          left: new JsonArea(document.getElementsByClassName('json-input-left')[0]),
          right: new JsonArea(document.getElementsByClassName('json-input-right')[0])
        };

        $scope.areas.left.editor = CodeMirror.fromTextArea($scope.areas.left.element, {
            mode: 'javascript', json: true, readOnly: true
        });

        $scope.areas.right.editor = CodeMirror.fromTextArea($scope.areas.right.element, {
            mode: 'javascript', json: true, readOnly: true
        });
      }

      $scope.showDiff = true;
      $timeout(function(){
        $scope.areas.left.setValue($scope.lastClassroomJsonPublished);
        $scope.areas.right.setValue($scope.lastClassroomJsonSaved);
        var left = $scope.areas.left.parse($scope.areas.left.editor.getValue()),
          right = $scope.areas.right.parse($scope.areas.right.editor.getValue());
        var delta = jsondiffpatch.diff(left, right);
        document.getElementsByClassName('visualdiff')[0].innerHTML = jsondiffpatch.formatters.html.format(delta, left);
      },100);
    };

    $scope.toggleFullDiff = function (){
      $scope.isNormalDiff = !$scope.isNormalDiff;
      if($scope.isNormalDiff){
        $('.jsondiffpatch-unchanged').addClass('jsondiffpatch-showing');
        $('.jsondiffpatch-unchanged').removeClass('jsondiffpatch-unchanged');
      } else {
        $('.jsondiffpatch-showing').addClass('jsondiffpatch-unchanged');
        $('.jsondiffpatch-showing').removeClass('jsondiffpatch-showing');
      }
    };

  }
]);


app.component('confirmModal', {
  templateUrl: 'confirmModal.html',
  bindings: {
    resolve: '<',
    close: '&',
    dismiss: '&'
  },
  controller: [
    function () {
      var $ctrl = this;

      $ctrl.$onInit = function () {
      };

      $ctrl.ok = function () {
          $ctrl.close({$value: 'ok'});
      };

      $ctrl.cancel = function () {
          $ctrl.dismiss({$value: 'cancel'});
      };
    }
  ]
});


app.component('addLectureModal', {
  templateUrl: 'addLectureModal.html',
  bindings: {
    resolve: '<',
    close: '&',
    dismiss: '&'
  },
  controller: [
    'LECTURE_TYPES',
    function (LECTURE_TYPES) {
      var $ctrl = this;

      $ctrl.$onInit = function () {
          $ctrl.LECTURE_TYPES = angular.copy(LECTURE_TYPES);
          if($ctrl.resolve.hasFinalExamLecture){
            $ctrl.LECTURE_TYPES.splice($ctrl.LECTURE_TYPES.indexOf("finalExam"),1);
          }
      };

      $ctrl.ok = function () {
        if($ctrl.lectureType && $ctrl.lectureType !== 'lectureTypeSeparator'){
          $ctrl.close({$value: $ctrl.lectureType});
        }
      };

      $ctrl.cancel = function () {
        $ctrl.dismiss({$value: 'cancel'});
      };
    }
  ]
});


app.component('chooseExistingFileModal', {
  templateUrl: 'chooseExistingFileModal.html',
  bindings: {
    resolve: '<',
    close: '&',
    dismiss: '&'
  },
  controller: [
    function () {
      var $ctrl = this;

      $ctrl.$onInit = function () {
        $ctrl.files = [];
        for(var name in $ctrl.resolve.files){
          var file = $ctrl.resolve.files[name],
            isUploaded = (file.type === 'uploaded' && file.uploadedURL);
          if(isUploaded){
            var isVideoUploader = ($ctrl.resolve.uploaderType === 'video'),
              isVideoFile = (file.uploadedURL.split('.').pop() === 'mp4'),
            isAllowedExtension = (isVideoUploader && isVideoFile) || (!isVideoUploader && !isVideoFile);
            if(isAllowedExtension){
              var found = false;
              for(var i = 0; i < $ctrl.files.length; i++){
                if($ctrl.files[i].uploadedURL === file.uploadedURL){
                  found = true;
                  break;
                }
              }
              if(!found){
                file = angular.copy(file);
                delete file.hostedURL;
                $ctrl.files.push(file);
              }
            }
          }
        }
      };

      $ctrl.getPrefixURL = function() {
        var prefixURL = '';
        if(location.hostname === 'localhost'){
          prefixURL = 'http://localhost:8888';
        }
        prefixURL += $ctrl.resolve.files._baseURL;
        return prefixURL;
      };

      $ctrl.ok = function (selectedImage) {
        $ctrl.close({$value: selectedImage});
      };
    }
  ]
});


app.controller('FileController', [
  '$scope',
  'FileUploader',
  '$uibModal',
  function($scope, FileUploader, $uibModal) {

    $scope.init = function(modelAttribute, uploaderType){
      $scope.modelAttribute = modelAttribute;
      $scope.uploaderType = uploaderType;
      $scope.fileUUID = $scope.selectedNode.uuid + '_' + modelAttribute;
      $scope.root.files[$scope.fileUUID] = $scope.root.files[$scope.fileUUID] || {};
      $scope.root.files[$scope.fileUUID].type = $scope.root.files[$scope.fileUUID].type || 'uploaded';
      $scope.initializeUploader();
    };

    $scope.getPrefixURL = function() {
      var prefixURL = '';
      if($scope.root.files[$scope.fileUUID].type === 'uploaded'){
        if(location.hostname === 'localhost'){
          prefixURL = 'http://localhost:8888';
        }
        prefixURL += $scope.root.files._baseURL;
      }
      return prefixURL;
    };

    $scope.removeCurrentFile = function(){
      if($scope.root.files[$scope.fileUUID].type === 'uploaded'){
        delete $scope.root.files[$scope.fileUUID].uploadedURL;
        delete $scope.root.files[$scope.fileUUID].originalFileName;
      } else {
        delete $scope.root.files[$scope.fileUUID].hostedURL;
      }
    };

    $scope.chooseExistingFile = function(){
      var modalInstance = $uibModal.open({
        animation: true,
        component: 'chooseExistingFileModal',
        size: 'lg',
        resolve: {
          files: function(){
            return $scope.root.files;
          },
          uploaderType: function(){
            return $scope.uploaderType;
          }
        }
      });

      modalInstance.result.then(
        function (selectedFile) {
          $scope.root.files[$scope.fileUUID] = selectedFile;
        },
        function(){
        }
      );
    };

    $scope.$watch('selectedNode.uuid', function() {
      $scope.init($scope.modelAttribute, $scope.uploaderType);
    });

    $scope.initializeUploader = function(){
      var uploader = $scope.uploader = new FileUploader();
      uploader.queue = [];

      uploader.onAfterAddingFile = function(fileItem) {
        fileItem.disableMultipart = true;
        
        //TODO 'mpg, mpeg, mp4, mov, mkv'
        var allowedExtensions = $scope.uploaderType === 'video' ? 'mp4' : 'jpg, jpeg, gif, png',
          fileName = fileItem.file && fileItem.file.name,
          fileParts = fileName && fileName.split('.'),
          fileExtension = fileParts && fileParts.length > 1 && fileParts[fileParts.length - 1],
          isAllowedExtension = fileExtension && allowedExtensions.indexOf(fileExtension) >= 0;

        if(isAllowedExtension){
          if(uploader.queue.length > 1){
            uploader.queue.splice(0, 1);
          }
          uploader.queue[0].originalFileName = fileName;
          uploader.requestUploadPath = false;
          fileItem.file.name = $scope.uuid() + '.' + fileExtension;
          $scope.postMessageToParentFrame("requestUploadPath", fileItem.file.name);
        } else {
          $scope.sendKornellNotification("error", "Extensão do arquivo inválida. Extensões aceitas: " + allowedExtensions + '.');
          uploader.queue = [];
        }
      };

      uploader.onBeforeUploadItem = function(item) {
        if(!uploader.requestUploadPath) {
          $scope.sendKornellNotification("error", "Erro ao tentar fazer o upload do arquivo. Tente novamente mais tarde ou entre em contato com o suporte.");
          return;
        }
        if(uploader.queue.length == 1){
          uploader.queue[0].url = uploader.requestUploadPath;
          uploader.queue[0].uploader.url = uploader.requestUploadPath;
          uploader.queue[0].method = 'PUT';
          uploader.queue[0].headers = {};
          uploader.queue[0].headers["x-ms-blob-type"] = "BlockBlob";
          // if AWS
          var splitRequestUploadPath = uploader.requestUploadPath.split('.s3.amazonaws.com');
          if(splitRequestUploadPath.length > 1){
              uploader.queue[0].fullURL = splitRequestUploadPath[1].split('?AWS')[0];
          } else {
              // if azure
              var splitRequestUploadPath = uploader.requestUploadPath.split('.core.windows.net/data');
              if(splitRequestUploadPath.length > 1){
                  uploader.queue[0].fullURL = splitRequestUploadPath[1].split('?')[0];
              } else {
                  uploader.queue[0].fullURL = uploader.requestUploadPath;
              }
          }
          uploader.queue[0].fileUUID = $scope.fileUUID;
        }
      };

      uploader.onSuccessItem = function(fileItem, response, status, headers) {
        $scope.sendKornellNotification("success", "Upload concluído com sucesso.");
        var fullURLSplit = fileItem.fullURL.split('/');
        $scope.root.files[fileItem.fileUUID] = $scope.root.files[fileItem.fileUUID] || {};
        $scope.root.files[fileItem.fileUUID].uploadedURL = fullURLSplit.pop();
        $scope.root.files[fileItem.fileUUID].type = 'uploaded';
        $scope.root.files[fileItem.fileUUID].originalFileName = fileItem.originalFileName;
        $scope.root.files._baseURL = fullURLSplit.join('/')+'/';
        uploader.queue = [];
      };

      uploader.onErrorItem = function(fileItem, response, status, headers) {
        $scope.sendKornellNotification("error", "Erro ao tentar fazer o upload do arquivo. Tente novamente mais tarde ou entre em contato com o suporte.");
      };

      window.addEventListener('message',function(event) {
        if(event.data.type === 'responseUploadPath') {
            uploader.requestUploadPath = event.data.message;
            if(uploader.queue.length > 0){
              uploader.queue[0].upload();
            }
        }
      },false);

    };
  }
]);