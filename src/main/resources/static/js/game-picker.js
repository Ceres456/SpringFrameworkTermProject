/*
 * 게임 선택 칸: 한 페이지에 3 x 3(9개), 옆으로 끌면 다음 9개. 검색으로 목록을 좁힐 수 있다.
 * - 검색: 이름 / 장르 / 한글 초성(ㅂㄹㄹㅌ) 으로 찾는다. 찾은 게임만 다시 3 x 3 페이지로 나뉜다.
 * - 마우스는 드래그로 넘기고(손을 떼면 가까운 페이지로 정렬), 터치는 브라우저 기본 스크롤 + scroll-snap 을 쓴다.
 * - 드래그한 뒤에는 클릭이 게임 선택으로 이어지지 않게 막는다.
 * - 화살표 버튼 / 점 표시 / "1 / 5" 로 현재 위치를 보여 준다.
 */
(function () {
    'use strict';

    var PER_PAGE = 9;
    var DRAG_THRESHOLD = 6;     // px. 이만큼 움직여야 드래그로 본다 (그 전에는 그냥 클릭)

    var picker = document.querySelector('[data-game-picker]');
    var nav = document.querySelector('.picker-nav');
    var search = document.querySelector('.picker-search');
    var status = document.querySelector('.picker-status');
    var clearBtn = document.querySelector('[data-picker-clear]');   // 선택 항목일 때만 있다
    if (!picker || !nav) {
        return;
    }
    var grid = picker.querySelector('.game-grid');
    var allCards = Array.prototype.slice.call(picker.querySelectorAll('.game-card'));

    var prevBtn = nav.querySelector('[data-dir="-1"]');
    var nextBtn = nav.querySelector('[data-dir="1"]');
    var dots = nav.querySelector('.picker-dots');
    var count = nav.querySelector('.picker-count');
    var reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

    var visible = allCards.slice();   // 지금 검색 조건에 맞아 보이는 카드
    var pageCount = 1;

    // ---------- 검색 ----------

    var CHO = 'ㄱㄲㄴㄷㄸㄹㅁㅂㅃㅅㅆㅇㅈㅉㅊㅋㅌㅍㅎ';

    function normalize(s) {
        return (s || '').toLowerCase().replace(/\s+/g, '');
    }

    /** "발로란트" -> "ㅂㄹㄹㅌ" (한글이 아닌 글자는 그대로) */
    function chosung(s) {
        var out = '';
        for (var i = 0; i < s.length; i++) {
            var code = s.charCodeAt(i) - 0xAC00;
            out += (code >= 0 && code <= 11171) ? CHO.charAt(Math.floor(code / 588)) : s.charAt(i);
        }
        return out;
    }

    allCards.forEach(function (card) {
        var name = normalize(card.getAttribute('data-name'));
        card._key = name + '|' + normalize(card.getAttribute('data-genre'));
        card._cho = chosung(name);
    });

    function matches(card, q) {
        if (!q) {
            return true;
        }
        if (card._key.indexOf(q) !== -1) {
            return true;
        }
        // 초성만 입력한 경우
        return /^[ㄱ-ㅎ]+$/.test(q) && card._cho.indexOf(q) !== -1;
    }

    function selectedCard() {
        var input = picker.querySelector('input:checked');
        return input ? input.closest('.game-card') : null;
    }

    // ---------- 페이지 계산 ----------

    /** 한 페이지만큼의 가로 이동 거리 (= 다음 페이지 첫 카드와 첫 카드의 간격). 페이지가 2개 이상일 때만 쓴다. */
    function stride() {
        return visible[PER_PAGE].offsetLeft - visible[0].offsetLeft;
    }

    function maxScroll() {
        return picker.scrollWidth - picker.clientWidth;
    }

    function currentPage() {
        if (pageCount < 2) {
            return 0;
        }
        return Math.min(pageCount - 1, Math.max(0, Math.round(picker.scrollLeft / stride())));
    }

    var raf = 0;
    function updateNav() {
        raf = 0;
        var page = currentPage();
        count.textContent = (page + 1) + ' / ' + pageCount;
        dots.childNodes.forEach(function (d, idx) {
            d.className = idx === page ? 'on' : '';
        });
        prevBtn.disabled = picker.scrollLeft <= 1;
        nextBtn.disabled = picker.scrollLeft >= maxScroll() - 1;
    }
    function scheduleNav() {
        if (!raf) {
            raf = requestAnimationFrame(updateNav);
        }
    }

    function updateStatus() {
        var q = search ? search.value.trim() : '';
        var parts = [];
        if (q) {
            parts.push(visible.length === 0
                ? '"' + q + '"에 맞는 게임이 없어요.'
                : visible.length + '개의 게임을 찾았어요.');
        }
        // 고른 게임은 검색으로 가려져도 알 수 있게 늘 보여 준다
        var sel = selectedCard();
        if (sel) {
            parts.push('선택한 게임: ' + sel.getAttribute('data-name'));
        }
        if (clearBtn) {
            clearBtn.hidden = !sel;
        }
        status.textContent = parts.join('  ·  ');
        status.hidden = parts.length === 0;
        status.classList.toggle('empty', q !== '' && visible.length === 0);
    }

    /** 검색어에 맞게 카드를 보이고 숨긴 뒤, 3 x 3 페이지와 이동 표시를 다시 만든다. */
    function refresh() {
        var q = normalize(search ? search.value : '');
        visible = allCards.filter(function (card) {
            var show = matches(card, q);
            card.hidden = !show;
            return show;
        });
        allCards.forEach(function (card) { card.classList.remove('page-start'); });
        visible.forEach(function (card, i) {
            if (i % PER_PAGE === 0) {
                card.classList.add('page-start');
            }
        });

        // 마지막 페이지가 9개보다 적어도 그 페이지가 맨 앞에 맞춰 서도록 빈 칸으로 채운다
        grid.querySelectorAll('.game-filler').forEach(function (f) { f.remove(); });
        pageCount = Math.max(1, Math.ceil(visible.length / PER_PAGE));
        for (var f = visible.length; f < pageCount * PER_PAGE && pageCount > 1; f++) {
            var filler = document.createElement('span');
            filler.className = 'game-filler';
            filler.setAttribute('aria-hidden', 'true');
            grid.appendChild(filler);
        }

        dots.textContent = '';
        for (var i = 0; i < pageCount; i++) {
            dots.appendChild(document.createElement('i'));
        }
        nav.hidden = pageCount < 2;

        picker.classList.remove('gliding');
        picker.scrollLeft = 0;
        picker.classList.toggle('single', pageCount < 2);
        updateStatus();
        updateNav();
    }

    var glideTimer = 0;
    /** 해당 페이지로 부드럽게 이동. 이동 중에는 scroll-snap 이 끼어들지 않게 잠시 끈다. */
    function goTo(page) {
        if (pageCount < 2) {
            return;
        }
        page = Math.min(pageCount - 1, Math.max(0, page));
        var left = Math.min(maxScroll(), page * stride());
        picker.classList.add('gliding');
        picker.scrollTo({left: left, behavior: reduceMotion ? 'auto' : 'smooth'});
        clearTimeout(glideTimer);
        glideTimer = setTimeout(function () {
            picker.classList.remove('gliding');
            scheduleNav();
        }, reduceMotion ? 0 : 450);
    }

    prevBtn.addEventListener('click', function () { goTo(currentPage() - 1); });
    nextBtn.addEventListener('click', function () { goTo(currentPage() + 1); });
    picker.addEventListener('scroll', scheduleNav, {passive: true});
    window.addEventListener('resize', scheduleNav);
    picker.addEventListener('change', updateStatus);

    if (clearBtn) {
        clearBtn.addEventListener('click', function () {
            picker.querySelectorAll('input:checked').forEach(function (input) { input.checked = false; });
            updateStatus();
        });
    }

    if (search) {
        search.hidden = false;
        search.addEventListener('input', refresh);
        search.addEventListener('keydown', function (e) {
            if (e.key === 'Enter') {
                e.preventDefault();         // 검색창에서 Enter 를 눌러도 폼이 저장되지 않게
            } else if (e.key === 'Escape' && search.value) {
                search.value = '';
                refresh();
            }
        });
    }

    // ---------- 마우스 드래그 ----------

    var drag = null;
    var suppressClick = false;

    picker.addEventListener('pointerdown', function (e) {
        if (e.pointerType !== 'mouse' || e.button !== 0) {
            return;                         // 터치/펜은 기본 스크롤에 맡긴다
        }
        clearTimeout(glideTimer);
        picker.classList.remove('gliding');
        drag = {x: e.clientX, left: picker.scrollLeft, moved: false, id: e.pointerId, lastX: e.clientX, t: e.timeStamp, v: 0};
    });

    picker.addEventListener('pointermove', function (e) {
        if (!drag || e.pointerId !== drag.id) {
            return;
        }
        var dx = e.clientX - drag.x;
        if (!drag.moved) {
            if (Math.abs(dx) < DRAG_THRESHOLD) {
                return;
            }
            drag.moved = true;
            picker.classList.add('dragging');
            picker.setPointerCapture(e.pointerId);
        }
        var dt = Math.max(1, e.timeStamp - drag.t);
        drag.v = (e.clientX - drag.lastX) / dt;      // px/ms, 살짝 튕기는 동작 판별용
        drag.lastX = e.clientX;
        drag.t = e.timeStamp;
        picker.scrollLeft = drag.left - dx;
    });

    function endDrag(e) {
        if (!drag || e.pointerId !== drag.id) {
            return;
        }
        var d = drag;
        drag = null;
        if (!d.moved) {
            return;
        }
        picker.classList.remove('dragging');
        if (picker.hasPointerCapture(e.pointerId)) {
            picker.releasePointerCapture(e.pointerId);
        }
        // 이 뒤에 오는 click 은 드래그의 일부이므로 게임 선택으로 이어지지 않게 한다
        suppressClick = true;
        setTimeout(function () { suppressClick = false; }, 0);
        if (pageCount < 2) {
            return;
        }

        // 절반 넘게 끌었거나 빠르게 튕겼으면 다음(이전) 페이지로, 아니면 제자리로
        var page = picker.scrollLeft / stride();
        var base = Math.floor(page);
        var target;
        if (d.v < -0.5) {
            target = base + 1;
        } else if (d.v > 0.5) {
            target = base;
        } else {
            target = Math.round(page);
        }
        if (page - base < 0.0001) {
            target = base;
        }
        goTo(target);
    }
    picker.addEventListener('pointerup', endDrag);
    picker.addEventListener('pointercancel', endDrag);

    picker.addEventListener('click', function (e) {
        if (suppressClick) {
            e.preventDefault();
            e.stopPropagation();
        }
    }, true);
    picker.addEventListener('dragstart', function (e) { e.preventDefault(); });

    // ---------- 시작 ----------

    refresh();

    // 이미 고른 게임(수정 화면, 검증 실패 후)이 있으면 그 페이지로 맞춰 둔다
    var checked = selectedCard();
    if (checked && pageCount > 1) {
        var index = visible.indexOf(checked);
        picker.scrollLeft = Math.min(maxScroll(), Math.floor(index / PER_PAGE) * stride());
        updateNav();
    }
})();
