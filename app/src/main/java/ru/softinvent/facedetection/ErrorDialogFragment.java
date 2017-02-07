package ru.softinvent.facedetection;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialogFragment;


/**
 * Фрагмент диалога с сообщением о недоступности сервисов Google Play.
 * Единственная кнопка предусматривает выход из приложения.
 */
public class ErrorDialogFragment extends AppCompatDialogFragment {
    private static final String ARG_MESSAGE = "ARG_MESSAGE";
    private OnDialogButtonClickListener listener;

    public ErrorDialogFragment() {
        // Required empty public constructor
    }

    public static ErrorDialogFragment getInstance(String message) {
        Bundle args = new Bundle();
        args.putString(ARG_MESSAGE, message);
        ErrorDialogFragment fragment = new ErrorDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = getArguments();
        return new AlertDialog.Builder(getActivity())
                .setTitle("Ошибка")
                .setMessage(args.getString(ARG_MESSAGE))
                .setPositiveButton("Выход", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (listener != null) {
                            listener.onExitClick();
                        }
                    }
                })
                .create();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnDialogButtonClickListener) {
            listener = (OnDialogButtonClickListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement OnDialogButtonClickListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    /**
     * Этот интерфейс должен быть реализован в активити, использующей фрагмент диалога.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnDialogButtonClickListener {
        void onExitClick();
    }
}
