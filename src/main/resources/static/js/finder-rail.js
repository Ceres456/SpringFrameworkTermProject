/*
 * 파티 찾기: 왼쪽 게임 목록 상자의 높이를 오른쪽 파티 카드의 "3번째 줄 아래 끝"에 맞춘다.
 * 줄 수는 화면 폭(카드 열 수)에 따라 달라지므로 실제 위치를 재서 정한다.
 * - 카드가 3줄보다 적으면 있는 마지막 줄 끝까지, 너무 낮아지지 않게 최소 높이를 둔다.
 * - 카드가 하나도 없으면 손대지 않는다 (CSS 기본 높이).
 * - 좁은 화면(가로 띠)에서는 쓰지 않는다.
 */
(function () {
    'use strict';

    var ROWS = 3;
    var MIN_HEIGHT = 320;       // px

    var finder = document.querySelector('.finder');
    var rail = document.querySelector('.game-rail');
    var grid = document.querySelector('.party-grid');
    if (!finder || !rail) {
        return;
    }
    var wide = window.matchMedia('(min-width: 901px)');

    function reset() {
        rail.style.height = '';
        rail.style.maxHeight = '';
    }

    function fit() {
        if (!wide.matches || !grid) {
            reset();
            return;
        }
        var cards = grid.querySelectorAll('.party-card');
        if (cards.length === 0) {
            reset();
            return;
        }
        var scrollY = window.scrollY || window.pageYOffset;
        var top = finder.getBoundingClientRect().top + scrollY;     // 상자가 놓이는 자리의 위쪽 끝

        // 같은 줄의 카드는 위쪽 위치가 같다. 줄마다 가장 아래쪽 끝을 모은다.
        var rows = [];
        cards.forEach(function (card) {
            var r = card.getBoundingClientRect();
            var rowTop = Math.round(r.top + scrollY);
            var bottom = r.bottom + scrollY;
            var row = rows.filter(function (x) { return Math.abs(x.top - rowTop) < 2; })[0];
            if (!row) {
                rows.push({top: rowTop, bottom: bottom});
            } else {
                row.bottom = Math.max(row.bottom, bottom);
            }
        });
        rows.sort(function (a, b) { return a.top - b.top; });
        var target = rows[Math.min(ROWS, rows.length) - 1];

        var height = Math.max(MIN_HEIGHT, Math.round(target.bottom - top));
        rail.style.maxHeight = 'none';      // 화면 높이로 잘리지 않게. 길면 상자 안에서 스크롤된다.
        rail.style.height = height + 'px';
    }

    var queued = 0;
    function schedule() {
        clearTimeout(queued);
        queued = setTimeout(fit, 30);
    }

    window.addEventListener('resize', schedule);
    window.addEventListener('load', schedule);
    wide.addEventListener('change', schedule);
    if (grid && 'ResizeObserver' in window) {
        // 카드 이미지가 늦게 불러와져 높이가 바뀌어도 따라간다
        new ResizeObserver(schedule).observe(grid);
    }
    fit();
})();
