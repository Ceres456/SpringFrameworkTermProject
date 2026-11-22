/* 오류 화면의 "이전 페이지" 버튼. 이전 기록이 없으면(바로 이 주소로 들어온 경우) 파티 찾기로 보낸다. */
(function () {
    'use strict';

    var button = document.querySelector('[data-back]');
    if (!button) {
        return;
    }
    button.addEventListener('click', function () {
        if (window.history.length > 1) {
            window.history.back();
        } else {
            window.location.href = '/parties';
        }
    });
})();
