'use strict';

var app = angular.module('knl');

app.constant('LECTURE_TYPES', [
  {
    type: "text",
    name: "Texto"
  },
  {
    type: "bubble",
    name: "Balões"
  },
  {
    type: "image",
    name: "Imagem"
  },
  {
    type: "faq",
    name: "Dúvidas"
  },
  {
    type: "lectureTypeSeparator",
    name: "-----------------------------------------"
  },
  {
    type: "video",
    name: "Vídeo"
  },
  {
    type: "youtube",
    name: "YouTube"
  },
  {
    type: "vimeo",
    name: "Vimeo"
  },
  {
    type: "lectureTypeSeparator",
    name: "-----------------------------------------"
  },
  {
    type: "question",
    name: "Exercício de Aprendizado"
  },
  {
    type: "finalExam",
    name: "Avaliação Final"
  }
]);

