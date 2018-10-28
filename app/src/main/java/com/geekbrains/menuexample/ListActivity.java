package com.geekbrains.menuexample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.SparseBooleanArray;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.PopupMenu;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ListActivity extends AppCompatActivity {

    @BindView(R.id.list_button)
    Button notesMenuButton;
    @BindView(R.id.list)
    ListView listView;

    List<String> elements;
    ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notes);
        ButterKnife.bind(this);
        elements = new ArrayList<String>();

        // Добавление элементов для заполнения списка
        populateList();

        // Создаем адаптер с item checkmarks
        adapter = new ArrayAdapter<String>
                (this, android.R.layout.simple_list_item_multiple_choice, elements);

        // Устанавливаем адаптер списку
        listView.setAdapter(adapter);

        // Ставим режим на listView с правом чекать и анчекать item в списке
        listView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);

        // Ставим listener на нажатие кнопки menu под списком
        notesMenuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Создаем PopUp меню
                PopupMenu popup = new PopupMenu(v.getContext(), v);
                MenuInflater inflater = popup.getMenuInflater();

                // Проверка на то выбран ли хоть один елемент или нет
                if (listView.getCheckedItemCount() == 0) {
                    // Ни один элемент не выбран в списке
                    inflater.inflate(R.menu.main_menu, popup.getMenu());

                    // Listener выбора элемента в Popup menu
                    popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem menuItem) {
                            switch (menuItem.getItemId()) {
                                case R.id.menu_add:
                                    addElement();
                                    return true;
                                case R.id.menu_clear:
                                    clearList();
                                    return true;
                                case R.id.menu_reset:
                                    resetList();
                                    return true;
                                default:
                                    return false;
                            }
                        }
                    });

                    // Показать PopUp
                    popup.show();
                } else {
                    // Как мимиум один элемент выбран в списке
                    final SparseBooleanArray checkedItemList = listView.getCheckedItemPositions();

                    // Проверка на флаги из-за того что когда ставишь и убираешь чекмарк,
                    //  или удаляешь/изменяешь с чекмарком, item остается в checkedItemList
                    for (int i = 0; i < checkedItemList.size(); i++) {
                        if (checkedItemList.indexOfValue(false) != -1)
                            checkedItemList.removeAt(checkedItemList.indexOfValue(false));
                        else break;
                    }


                    inflater.inflate(R.menu.context_menu, popup.getMenu());

                    // Listener выбора элемента в Popup menu
                    popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            switch (item.getItemId()) {
                                case R.id.menu_edit:
                                    for (int i = 0; i < checkedItemList.size(); i++) {
                                        editElement(checkedItemList.keyAt(i));
                                        listView.setItemChecked(checkedItemList.keyAt(i), false);
                                    }
                                    checkedItemList.clear();
                                    return true;
                                case R.id.menu_delete:
                                    for (int i = 0; i < checkedItemList.size(); i++) {
                                        deleteElement(checkedItemList.keyAt(i) - i);
                                        listView.setItemChecked(checkedItemList.keyAt(i), false);
                                    }
                                    checkedItemList.clear();
                                    return true;
                                default:
                                    return false;
                            }
                        }
                    });

                    // Показать PopUp
                    popup.show();
                }
            }
        });

        // регестрируем контекстное меню на список.
        registerForContextMenu(listView);
    }

    // Переопределение метода создания меню. Этот callback вызывается всегда, но он пустой, здесь мы
    //переопределением говорим системе создать наше меню.
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    // Метод вызывается по нажатию на любой пункт меню. В качестве агрумента приходит item меню.
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // обработка нажатий
        switch (item.getItemId()) {
            case R.id.menu_add:
                addElement();
                return true;
            case R.id.menu_clear:
                clearList();
                return true;
            case R.id.menu_reset:
                resetList();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // Метод, который вызывается не всего один раз как было с option menu, а каждый раз перед тем,
    // как context-ное меню будет показано.
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.context_menu, menu);
    }

    // Метод вызывается по долгому нажатию на любой пункт меню. В качестве агрумента приходит item меню.
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        switch (item.getItemId()) {
            case R.id.menu_edit:
                editElement(info.position);
                listView.setItemChecked(info.position,false);
                return true;
            case R.id.menu_delete:
                deleteElement(info.position);
                listView.setItemChecked(info.position,false);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    // Метод для популяции нового, дефолтного списка
    private void populateList() {
        //Создаем массив элементов для списка
        for (int i = 0; i < 5; i++) {
            elements.add("Element " + i);
        }
    }

    // Метод очищает лист полностью.
    private void clearList() {
        elements.clear();
        adapter.notifyDataSetChanged();
    }

    // Метод для сброса списка
    private void resetList() {
        clearList();
        populateList();
    }

    // Метод добавляет элемент в список.
    private void addElement() {
        elements.add("New element");
        adapter.notifyDataSetChanged();
    }

    // Метод переписывает текст пункта меню на другой.
    private void editElement(int id) {
        elements.set(id, "Edited");
        adapter.notifyDataSetChanged();
    }

    // Метод удаляет пункт из меню.
    private void deleteElement(int id) {
        elements.remove(id);
        adapter.notifyDataSetChanged();
    }
}
