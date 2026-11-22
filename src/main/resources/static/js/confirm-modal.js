/*
 * 사이트 디자인의 확인창. 브라우저 기본 confirm() 대신 쓴다.
 *
 * 폼에 속성을 붙이면 제출 전에 확인창이 뜬다.
 *   data-confirm="메시지"            필수. 줄바꿈(&#10;)이 있으면 첫 줄은 제목, 나머지는 설명으로 보여 준다.
 *   data-confirm-ok="삭제"           확인 버튼 글자 (기본 "확인")
 *   data-confirm-tone="danger"       되돌릴 수 없는 작업이면 확인 버튼을 빨갛게, 처음 포커스는 "취소"에 둔다.
 *
 * 닫기: 취소 버튼 / Esc / 바깥(어두운 배경) 클릭.  Tab 키는 창 안에서만 돈다.
 */
(function () {
    'use strict';

    var backdrop = null;
    var lastFocus = null;
    var pending = null;          // {form, submitter}

    function build() {
        backdrop = document.createElement('div');
        backdrop.className = 'modal-backdrop';
        backdrop.hidden = true;
        backdrop.innerHTML =
            '<div class="modal" role="alertdialog" aria-modal="true" aria-labelledby="modal-title" aria-describedby="modal-desc">' +
            '  <p class="modal-kicker">GameParty</p>' +
            '  <h3 class="modal-title" id="modal-title"></h3>' +
            '  <p class="modal-desc" id="modal-desc"></p>' +
            '  <div class="modal-actions">' +
            '    <button type="button" class="btn small ghost" data-modal-cancel>취소</button>' +
            '    <button type="button" class="btn small" data-modal-ok>확인</button>' +
            '  </div>' +
            '</div>';
        document.body.appendChild(backdrop);

        backdrop.addEventListener('mousedown', function (e) {
            if (e.target === backdrop) {
                close();
            }
        });
        backdrop.querySelector('[data-modal-cancel]').addEventListener('click', close);
        backdrop.querySelector('[data-modal-ok]').addEventListener('click', accept);
        document.addEventListener('keydown', function (e) {
            if (backdrop.hidden) {
                return;
            }
            if (e.key === 'Escape') {
                e.preventDefault();
                close();
            } else if (e.key === 'Tab') {
                // 창 밖으로 포커스가 나가지 않게 두 버튼 사이에서만 돌린다
                var buttons = backdrop.querySelectorAll('button');
                var first = buttons[0];
                var last = buttons[buttons.length - 1];
                if (e.shiftKey && document.activeElement === first) {
                    e.preventDefault();
                    last.focus();
                } else if (!e.shiftKey && document.activeElement === last) {
                    e.preventDefault();
                    first.focus();
                }
            }
        });
    }

    function open(form, submitter) {
        if (!backdrop) {
            build();
        }
        pending = {form: form, submitter: submitter};
        lastFocus = document.activeElement;

        var lines = (form.getAttribute('data-confirm') || '').split('\n');
        backdrop.querySelector('.modal-title').textContent = lines[0];
        var desc = backdrop.querySelector('.modal-desc');
        desc.textContent = lines.slice(1).join('\n');
        desc.hidden = lines.length < 2;

        var danger = form.getAttribute('data-confirm-tone') === 'danger';
        var ok = backdrop.querySelector('[data-modal-ok]');
        ok.textContent = form.getAttribute('data-confirm-ok') || '확인';
        ok.classList.toggle('danger', danger);
        backdrop.querySelector('.modal').classList.toggle('danger', danger);

        backdrop.hidden = false;
        document.body.classList.add('modal-open');
        // 되돌릴 수 없는 작업은 실수로 Enter 를 눌러도 취소되도록 "취소"에 먼저 둔다
        (danger ? backdrop.querySelector('[data-modal-cancel]') : ok).focus();
    }

    function close() {
        if (!backdrop || backdrop.hidden) {
            return;
        }
        backdrop.hidden = true;
        document.body.classList.remove('modal-open');
        pending = null;
        if (lastFocus && document.contains(lastFocus)) {
            lastFocus.focus();
        }
    }

    function accept() {
        var target = pending;
        close();
        if (!target) {
            return;
        }
        target.form.setAttribute('data-confirmed', 'true');
        if (target.form.requestSubmit) {
            target.form.requestSubmit(target.submitter || undefined);
        } else {
            target.form.submit();
        }
    }

    // 캡처 단계에서 가로채야 다른 처리보다 먼저 막을 수 있다
    document.addEventListener('submit', function (e) {
        var form = e.target;
        if (!form.hasAttribute || !form.hasAttribute('data-confirm')) {
            return;
        }
        if (form.getAttribute('data-confirmed') === 'true') {
            form.removeAttribute('data-confirmed');     // 확인을 마친 제출은 그대로 통과
            return;
        }
        e.preventDefault();
        open(form, e.submitter);
    }, true);
})();
