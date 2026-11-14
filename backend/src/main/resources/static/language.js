// Language choice at "/" (spec 004, FR-010 and FR-011). Without JavaScript the options stay plain links.
(function () {
  'use strict';

  var COOKIE = 'veredito-lang';
  var ONE_YEAR = 31536000;
  var BY_LANGUAGE = { pt: 'pt-BR', en: 'en-US', es: 'es-ES' };

  /** The first browser language that is Portuguese, English or Spanish, whatever the region; else pt-BR. */
  function preselected(languages) {
    for (var i = 0; i < languages.length; i++) {
      var language = String(languages[i]).toLowerCase().split('-')[0];
      if (BY_LANGUAGE.hasOwnProperty(language)) {
        return BY_LANGUAGE[language];
      }
    }
    return 'pt-BR';
  }

  function save(locale) {
    document.cookie = COOKIE + '=' + locale + '; Path=/; Max-Age=' + ONE_YEAR + '; SameSite=Lax';
  }

  var languages = navigator.languages && navigator.languages.length ? navigator.languages : [navigator.language || ''];
  var choice = preselected(languages);
  var links = document.querySelectorAll('a[hreflang]');

  for (var i = 0; i < links.length; i++) {
    var link = links[i];
    var locale = link.getAttribute('hreflang');
    if (locale === choice) {
      link.setAttribute('aria-current', 'true');
      link.focus();
    }
    link.addEventListener('click', function (event) {
      save(event.currentTarget.getAttribute('hreflang'));
    });
  }
})();
