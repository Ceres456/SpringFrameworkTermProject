/*
 * 파티 상세 오른쪽 "파티 찾기" 목록: 8개씩 최대 3페이지, ‹ › 버튼으로 넘긴다.
 * - 8개 이하이면 넘김 버튼을 숨긴다.
 * - 페이지를 넘겨도 칸 높이가 들썩이지 않도록, 가장 긴 페이지에 높이를 맞춰 둔다.
 * - 스크립트가 없으면 목록이 그대로 전부 보인다.
 */
(function () {
    'use strict';

    var PER_PAGE = 8;
    var MAX_PAGES = 3;

    var list = document.querySelector('[data-other-list]');
    var pager = document.querySelector('.other-pager');
    if (!list || !pager) {
        return;
    }
    var items = Array.prototype.slice.call(list.children).slice(0, PER_PAGE * MAX_PAGES);
    // 3페이지(24개)를 넘는 항목은 아예 숨긴다
    Array.prototype.slice.call(list.children).slice(PER_PAGE * MAX_PAGES).forEach(function (li) { li.hidden = true; });

    var pageCount = Math.min(MAX_PAGES, Math.ceil(items.length / PER_PAGE));
    if (pageCount < 2) {
        return;
    }
    var prevBtn = pager.querySelector('[data-dir="-1"]');
    var nextBtn = pager.querySelector('[data-dir="1"]');
    var count = pager.querySelector('.other-count');
    var page = 0;

    function show(p) {
        items.forEach(function (li, i) {
            li.hidden = Math.floor(i / PER_PAGE) !== p;
        });
    }

    // 가장 높은 페이지의 높이를 재서 목록 최소 높이로 잡는다
    var tallest = 0;
    for (var p = 0; p < pageCount; p++) {
        show(p);
        tallest = Math.max(tallest, list.offsetHeight);
    }
    list.style.minHeight = tallest + 'px';

    function render() {
        show(page);
        count.textContent = (page + 1) + ' / ' + pageCount;
        prevBtn.disabled = page === 0;
        nextBtn.disabled = page === pageCount - 1;
    }

    prevBtn.addEventListener('click', function () { page = Math.max(0, page - 1); render(); });
    nextBtn.addEventListener('click', function () { page = Math.min(pageCount - 1, page + 1); render(); });
    pager.hidden = false;
    render();
})();
