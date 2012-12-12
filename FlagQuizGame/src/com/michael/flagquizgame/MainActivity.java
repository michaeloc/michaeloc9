package com.michael.flagquizgame;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.res.AssetManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SearchView.OnCloseListener;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class MainActivity extends Activity {
	
	private static final String TAG="FlagQuizGame Activity";//registrar mensagens de erro
	
	private List<String> fileNameList;
	private List<String> quizCurrentriesList;
	private Map<String, Boolean> regionsMap;
	private String correctAnswer;
	private int totalGuesses;
	private int correctAnswers;
	private int guessRowns;
	private Random random;
	private Handler handler;
	private Animation shakeAnimation;
	
	private TextView answerTextView;
	private TextView questionNumberTextView;
	private ImageView flagImageView;
	private TableLayout  buttonTableLayout;
	

	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		fileNameList = new ArrayList<String>();
		
		quizCurrentriesList = new ArrayList<String>();
		
		regionsMap = new HashMap<String, Boolean>();
		
		guessRowns = 1; //Exibe inicialmente apenas uma fileira de buttons
		
		random = new Random();
		
		handler = new Handler(); // Usado para atrasar o carregamento da bandeira
		
		shakeAnimation = AnimationUtils.loadAnimation(this,R.animator.incorrect_shake);
		
		shakeAnimation.setRepeatCount(3);
		
		String [] regionsName = getResources().getStringArray(R.array.regionsList);
		
		for(String region: regionsName)
			regionsMap.put(region,true);
		
		answerTextView = (TextView) findViewById(R.id.answerTextView);
		
		flagImageView = (ImageView) findViewById(R.id.flagImageView);
		
		buttonTableLayout = (TableLayout) findViewById(R.id.buttonTableLayout);
		
		questionNumberTextView = (TextView) findViewById(R.id.questionNuberTextView);
		
		questionNumberTextView.setText(getResources().getString(R.string.question)+"1"
				
		+getResources().getString(R.string.of)+ " 10");
		
		resetQuiz();	
		
		
	}
	
	private void resetQuiz()
	{
		AssetManager assets= getAssets();
		
		fileNameList.clear();
		
		try
		{
			Set<String> regions = regionsMap.keySet();
			
			for (String region: regions)
			{
				if(regionsMap.get(region))
				{
					String [] paths = assets.list(region);
					
					for(String path: paths)
						fileNameList.add(path.replace(".png",""));
					
				}
				
			}
			
		}catch(IOException e)
		{
			Log.e(TAG, "Error loading image files names",e);
			
		}
		correctAnswers = 0;
		totalGuesses = 0;
		quizCurrentriesList.clear();
		
		int flagCounter = 1;
		
		int numberOfFlag = fileNameList.size();
		
		while(flagCounter<=10)
		{
			int randomIndex = random.nextInt(numberOfFlag);
			
			String fileName = fileNameList.get(randomIndex);
			
			if(!quizCurrentriesList.contains(fileName))
			{
				quizCurrentriesList.add(fileName);
				++flagCounter;
				
				
			}
			
		}
		loadNextFlag();
		
	}
	
	private void loadNextFlag()
	{
		String nextImageName = quizCurrentriesList.remove(0);
		
		correctAnswer = nextImageName;
		
		answerTextView.setText("");
		
		questionNumberTextView.setText(getResources().getString(R.string.question)+" "+(correctAnswers+1) + " " +
		getResources().getString(R.string.of)+" 10");
		
		String region = nextImageName.substring(0,nextImageName.indexOf('-'));
		
		AssetManager asset = getAssets();
		InputStream stream;
		
		try
		{
			stream = asset.open(region+"/"+nextImageName+".png");
			
			Drawable flag = Drawable.createFromStream(stream,nextImageName);
			
			flagImageView.setImageDrawable(flag);
			
			
		}catch(IOException e)
		{
			Log.e(TAG,"Error Loading"+ nextImageName,e);
			
		}
		
		for(int i = 0; i<buttonTableLayout.getChildCount();i++)
			((TableRow) buttonTableLayout.getChildAt(i)).removeAllViews();
		
		Collections.shuffle(fileNameList);
		
		int correct = fileNameList.indexOf(correctAnswer);
		
		fileNameList.add(fileNameList.remove(correct));
		
		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
		for(int i = 0;i<guessRowns;i++)
		{
			TableRow currentTableRow = getTableRow(i);
			
			for(int j = 0; j<3;j++)
			{
				Button newGuessButton = (Button)inflater.inflate(R.layout.guess_button,null); //Infla um botão
				String fileName = fileNameList.get((i*3)+j);
				newGuessButton.setText(getCountryName(fileName));
				
				newGuessButton.setOnClickListener(guessButtonListener);
				currentTableRow.addView(newGuessButton);
				
			}
						
		}
		int row = random.nextInt(guessRowns);
		int column = random.nextInt(3);
		TableRow randomTableRow = getTableRow(row);
		String countryName = getCountryName(correctAnswer);
		((Button)randomTableRow.getChildAt(column)).setText(countryName);
	}
	
	private TableRow getTableRow(int row)
	{
		return (TableRow) buttonTableLayout.getChildAt(row);
		
	}
	
	private String getCountryName(String name)
	{
		return name.substring(name.indexOf('-')+1).replace('-',' ');
		
	}

	private void submitedGuess(Button guessButton)
	{
		String guess = guessButton.getText().toString();
		String answer = getCountryName(correctAnswer);
		++totalGuesses;
		
		if(guess.equals(answer))
		{
			++correctAnswers;
			
			answerTextView.setText(answer+ "!");
			answerTextView.setTextColor(getResources().getColor(R.color.correct_answer));
			disableButton();
			
			if(correctAnswers == 10)
			{
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				
				builder.setTitle(R.string.reset_quiz);
				
				builder.setMessage(String.format("%d %s, %.02f%% %s",totalGuesses, getResources().getString(R.string.guesses),
						(1000/(double)totalGuesses),getResources().getString(R.string.correct)));
				builder.setCancelable(false);
				
				builder.setPositiveButton(R.string.reset_quiz,new DialogInterface.OnClickListener() 
				{
					
					@Override
					public void onClick(DialogInterface dialog, int id)
					{
						// TODO Auto-generated method stub
						resetQuiz();
						
					}
				});
				
				AlertDialog resetDialog = builder.create();
				
				resetDialog.show();
				
			}else
			{
				handler.postDelayed(new Runnable(){ //Implementando a intefarce Runnable

					@Override
					public void run() {
						
						loadNextFlag();
						// TODO Auto-generated method stub
						
					}
					
					
				}, 1000);
				
			}
					
		}else
		{
			flagImageView.startAnimation(shakeAnimation);
			answerTextView.setText(R.string.incorrect_answer);
			answerTextView.setTextColor(getResources().getColor(R.color.incorrect_answer));
			guessButton.setEnabled(false);
			
		}
		
	}
	private android.view.View.OnClickListener guessButtonListener = new android.view.View.OnClickListener() {
		
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			submitedGuess((Button)v);
		}
	};
	private void disableButton()//Desabilitando o botões
	{
		for(int i = 0; i<buttonTableLayout.getChildCount();i++)
		{
			TableRow tableRow = (TableRow) buttonTableLayout.getChildAt(i);
			for (int j = 0; j< tableRow.getChildCount();j++)
				tableRow.getChildAt(j).setEnabled(false);
			
		}
		
	}
	private final int CHOICES_MENU_ID = Menu.FIRST;
	private final int REGIONS_MENU_ID = Menu.FIRST+1;
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {//Sobrescrevendo 
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		
		menu.add(Menu.NONE,CHOICES_MENU_ID,Menu.NONE,R.string.choices);
		menu.add(Menu.NONE,REGIONS_MENU_ID,Menu.NONE, R.string.regions);
		return true;
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch(item.getItemId())
		{
		case CHOICES_MENU_ID:
			final String[] possibleChoices = getResources().getStringArray(R.array.guessesList);
			
			AlertDialog.Builder choiceBuilder = new AlertDialog.Builder(this);
			
			choiceBuilder.setTitle(R.string.choices);
			
			choiceBuilder.setItems(possibleChoices, new DialogInterface.OnClickListener() {
								
				@Override
				public void onClick(DialogInterface dialog, int which) 
				{
					guessRowns = Integer.parseInt(possibleChoices[which].toString())/3;//Retorna o número de linhas (3|6|9)
					
					resetQuiz();
					
				}
			});
			AlertDialog choicesDialog = choiceBuilder.create();
			
			choiceBuilder.show();
			
			return true;
			
		case REGIONS_MENU_ID:
			final String [] regionNames = regionsMap.keySet().toArray(new String [regionsMap.size()]);
			boolean [] regionEnable = new boolean [regionsMap.size()]; //utilizado para marcar quais regiões estão ativas
			for (int i = 0; i<regionEnable.length;i++)
				regionEnable[i] = regionsMap.get(regionNames[i]);
			AlertDialog.Builder regionsBuilder = new AlertDialog.Builder(this);
			regionsBuilder.setTitle(R.string.regions);
			
			String [] names = new String[regionNames.length];
			for (int j = 0; j < names.length; j++)
				names[j] = regionNames[j].replace('_',' ');
			regionsBuilder.setMultiChoiceItems(names,regionEnable, new DialogInterface.OnMultiChoiceClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which, boolean isChecked) {
					// TODO Auto-generated method stub
					regionsMap.put(regionNames[which], isChecked);
					
				}
			});
			regionsBuilder.setPositiveButton(R.string.reset_quiz, new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub
					resetQuiz();
				}
			});
			AlertDialog regionDialog = regionsBuilder.create();
			regionDialog.show();
			
			return true;
		}
		
		return super.onOptionsItemSelected(item);
	}
	
}
