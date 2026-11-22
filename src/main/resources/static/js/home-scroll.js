/*
 * 메인 화면: 휠을 한 번 내리면 다음 화면의 시작 위치까지 부드럽게 미끄러져 내려간다.
 *
 * CSS 스크롤 스냅만으로는 휠 한 칸(약 100px)이 "가장 가까운 지점"으로 되돌아가 버려서,
 * 한 번의 입력을 한 화면의 이동으로 바꾸는 이 스크립트를 함께 쓴다.
 * 터치(스와이프)는 CSS 스냅이 그대로 맡는다.
 * "동작 줄이기" 설정을 켠 사람에게는 아무것도 하지 않고 일반 스크롤로 둔다.
 */
(function () {
    'use strict';

    var root = document.documentElement;
    if (!root.classList.contains('snap-page')) {
        return;
    }

    /*
     * 화면에 고정된 이미지 줄은 첫 화면에서는 전체가 보이고, 아래 화면에서는 양쪽 끝만 보인다.
     * 스크롤 위치에 맞춰 --full 을 1(전체)에서 0(양쪽 끝만)으로 낮춰 가림이 부드럽게 닫히게 한다.
     * 움직임이 아니라 표시 범위를 바꾸는 것이라 "동작 줄이기" 설정과 무관하게 동작한다.
     */
    /*
     * 첫 화면은 "창 높이 - 헤더 높이"로 꽉 채운다. 헤더 높이는 메뉴가 줄바꿈되면(좁은 화면, 로그인한 상태) 달라지므로
     * 고정 숫자 대신 실제 높이를 재서 --header-h 에 반영한다.
     */
    var header = document.querySelector('header.nav');
    function syncHeaderHeight() {
        if (header) {
            document.body.style.setProperty('--header-h', header.offsetHeight + 'px');
        }
    }
    syncHeaderHeight();
    window.addEventListener('resize', syncHeaderHeight);
    if (document.fonts && document.fonts.ready) {
        document.fonts.ready.then(syncHeaderHeight);   // 글꼴이 늦게 적용되면 높이가 바뀔 수 있다
    }

    var strip = document.querySelector('.edge-marquee');
    function updateReveal() {
        if (!strip) {
            return;
        }
        var progress = Math.min(Math.max(window.scrollY / window.innerHeight, 0), 1);
        strip.style.setProperty('--full', (1 - progress).toFixed(3));
    }
    window.addEventListener('scroll', updateReveal, { passive: true });
    window.addEventListener('resize', updateReveal);
    updateReveal();

    var reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)');
    var DURATION = 800;        // 한 화면을 넘어가는 시간(ms)
    var GESTURE_GAP = 140;     // 이 시간 동안 휠 입력이 끊겨야 새 입력으로 본다(관성 스크롤 무시)
    var animating = false;
    var lastWheelAt = 0;

    /** 한 화면씩 끊어 보는 지점: 맨 위, 그리고 각 소개 섹션의 시작 */
    function pageTops() {
        var tops = [0];
        var sections = document.querySelectorAll('.showcase, .about, .rules, .closing');
        for (var i = 0; i < sections.length; i++) {
            tops.push(Math.round(sections[i].getBoundingClientRect().top + window.scrollY));
        }
        return tops;
    }

    function maxScroll() {
        return Math.max(0, root.scrollHeight - window.innerHeight);
    }

    /** 지금 위치에서 한 화면 아래(+1) 또는 위(-1)의 목표 위치. 갈 곳이 없으면 null */
    function targetFrom(direction) {
        var tops = pageTops();
        var y = window.scrollY;
        var limit = maxScroll();
        var i;

        if (direction > 0) {
            for (i = 0; i < tops.length; i++) {
                if (tops[i] > y + 4) {
                    return Math.min(tops[i], limit);
                }
            }
            return null;
        }
        for (i = tops.length - 1; i >= 0; i--) {
            if (tops[i] < y - 4) {
                return tops[i];
            }
        }
        return null;
    }

    /** 화면 하나가 창보다 커서 아직 안쪽을 다 못 본 경우에는 일반 스크롤에 맡긴다. */
    function insideTallPage(direction) {
        var tops = pageTops();
        var y = window.scrollY;
        var cur = 0;
        for (var i = 0; i < tops.length; i++) {
            if (tops[i] <= y + 4) {
                cur = i;
            }
        }
        var pageBottom = cur + 1 < tops.length ? tops[cur + 1] : root.scrollHeight;
        if (direction > 0) {
            return y + window.innerHeight < pageBottom - 4;
        }
        return y > tops[cur] + 4;
    }

    function ease(p) {
        return p < 0.5 ? 4 * p * p * p : 1 - Math.pow(-2 * p + 2, 3) / 2;
    }

    function glideTo(targetY) {
        var startY = window.scrollY;
        var distance = targetY - startY;
        if (Math.abs(distance) < 1) {
            return;
        }
        animating = true;
        root.style.scrollSnapType = 'none';   // 이동 중에 스냅이 끼어들지 않게 잠시 끈다
        var startedAt = null;

        function step(now) {
            if (startedAt === null) {
                startedAt = now;
            }
            var p = Math.min((now - startedAt) / DURATION, 1);
            window.scrollTo({ top: startY + distance * ease(p), behavior: 'instant' });
            if (p < 1) {
                window.requestAnimationFrame(step);
            } else {
                animating = false;
                root.style.scrollSnapType = '';
            }
        }
        window.requestAnimationFrame(step);
    }

    function move(direction) {
        if (animating) {
            return;
        }
        var target = targetFrom(direction);
        if (target !== null) {
            glideTo(target);
        }
    }

    window.addEventListener('wheel', function (e) {
        if (reduceMotion.matches || e.ctrlKey || e.defaultPrevented) {
            return;
        }
        if (Math.abs(e.deltaY) < Math.abs(e.deltaX) || e.deltaY === 0) {
            return;                                   // 가로 스크롤은 건드리지 않는다
        }
        var direction = e.deltaY > 0 ? 1 : -1;
        if (insideTallPage(direction)) {
            return;
        }
        e.preventDefault();
        var now = Date.now();
        var newGesture = now - lastWheelAt > GESTURE_GAP;
        lastWheelAt = now;
        if (newGesture) {
            move(direction);
        }
    }, { passive: false });

    window.addEventListener('keydown', function (e) {
        if (reduceMotion.matches || e.altKey || e.ctrlKey || e.metaKey || e.shiftKey) {
            return;
        }
        var direction = 0;
        if (e.key === 'ArrowDown' || e.key === 'PageDown') {
            direction = 1;
        } else if (e.key === 'ArrowUp' || e.key === 'PageUp') {
            direction = -1;
        } else if (e.key === ' ' && (e.target === document.body || e.target === root)) {
            direction = 1;
        } else if (e.key === 'Home') {
            e.preventDefault();
            if (!animating) { glideTo(0); }
            return;
        } else if (e.key === 'End') {
            e.preventDefault();
            if (!animating) { glideTo(maxScroll()); }
            return;
        }
        if (direction === 0 || insideTallPage(direction)) {
            return;
        }
        e.preventDefault();
        move(direction);
    });

    /** "사이트 소개 보기" 같은 같은 페이지 안의 링크도 같은 방식으로 이동한다. */
    document.addEventListener('click', function (e) {
        var link = e.target.closest ? e.target.closest('a[href^="#"]') : null;
        if (!link || reduceMotion.matches) {
            return;
        }
        var id = link.getAttribute('href').slice(1);
        var target = id ? document.getElementById(id) : null;
        if (!target) {
            return;
        }
        e.preventDefault();
        if (!animating) {
            glideTo(Math.min(Math.round(target.getBoundingClientRect().top + window.scrollY), maxScroll()));
        }
    });
})();
