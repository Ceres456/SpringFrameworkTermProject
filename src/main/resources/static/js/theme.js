/*
 * 다크 / 화이트 모드 전환.
 * - 모든 페이지의 <head> 에서 바로 실행해, 화면이 그려지기 전에 저장된 테마를 적용한다 (깜빡임 방지).
 * - 선택은 브라우저(localStorage)에 저장한다. 저장된 값이 없으면 다크 모드.
 * - 헤더 오른쪽 위의 [data-theme-toggle] 버튼으로 바꾼다. 다른 탭에서 바꿔도 따라간다.
 */
(function () {
    'use strict';

    var KEY = 'gp-theme';
    var root = document.documentElement;

    function read() {
        try {
            var v = localStorage.getItem(KEY);
            return v === 'light' || v === 'dark' ? v : null;
        } catch (e) {
            return null;      // 저장소를 못 쓰는 환경(사생활 보호 모드 등)이어도 동작은 한다
        }
    }

    function write(v) {
        try {
            localStorage.setItem(KEY, v);
        } catch (e) {
            // 저장 실패: 이번 방문에서만 적용된다
        }
    }

    function apply(theme) {
        root.setAttribute('data-theme', theme);
    }

    /** 버튼 글자와 안내문을 현재 상태에 맞춘다: 다크일 때는 "화이트 모드", 화이트일 때는 "다크 모드" */
    function syncButtons() {
        var light = root.getAttribute('data-theme') === 'light';
        var next = light ? '다크 모드' : '화이트 모드';
        document.querySelectorAll('[data-theme-toggle]').forEach(function (button) {
            button.setAttribute('aria-label', next + '로 전환');
            button.title = next + '로 전환';
            var text = button.querySelector('.tt-text');
            if (text) {
                text.textContent = next;
            }
        });
    }

    apply(read() || 'dark');

    document.addEventListener('click', function (e) {
        var button = e.target.closest ? e.target.closest('[data-theme-toggle]') : null;
        if (!button) {
            return;
        }
        var theme = root.getAttribute('data-theme') === 'light' ? 'dark' : 'light';
        apply(theme);
        write(theme);
        syncButtons();
    });

    window.addEventListener('storage', function (e) {
        if (e.key === KEY && (e.newValue === 'light' || e.newValue === 'dark')) {
            apply(e.newValue);
            syncButtons();
        }
    });

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', syncButtons);
    } else {
        syncButtons();
    }
})();
