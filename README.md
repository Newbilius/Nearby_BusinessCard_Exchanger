# Обменник визитками [неудачный эксперимент]

Эксперимент с библиотекой Nearby от Google, приложение для обмена контактными данными.

Изначально был план сделать так:
-каждое приложение при старте начинает сообщать о себе как о сервере
-поиск другого сервера идёт по клику
-после коннекта отправляем сообщение и тут-же дисконнектимся

Но он не заработал, почему-то Nearby как-то плохо рвёт соединения, начиная после этого глючить.
Поэтому схема изменена... Но стабильной работы всё-равно не удалось добиться :-/
Теперь я понимаю, почему Nearby в общем-то не так уж и популярна.
